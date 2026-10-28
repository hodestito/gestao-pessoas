# 007 — Evento de inativação e dois tópicos Kafka

Estado: aprovado pelo responsável técnico e integrado à main no PR #5. O responsável técnico aprovou dois tópicos, preservando o atual. Data: 28/10/2026.

## Problema e decisão

DELETE /employees/{id} já alterava Employee de ACTIVE para INACTIVE com controle otimista e repetição idempotente. Faltava EmployeeStatusChangedEvent, exigido pelo enunciado, com publicação assíncrona, logs e histórico.

A inativação passa a registrar a mudança e o evento na mesma transação JPA, reutilizando Outbox, publicador e employee_events. Não há nova tabela nem alteração nas migrações existentes.

O responsável técnico escolheu manter employee-created.v1 e acrescentar employee-status-changed.v1. Isso preserva o contrato de criação e mantém consumidores com desserialização específica por tópico, ao custo de mais um tópico e ausência de ordem entre eles.

A alternativa seria um tópico comum para eventos do agregado, com chave employeeId. Poderia preservar a ordem das mensagens na partição, mas também exigiria uma estratégia de publicação que preservasse a sequência sob retries; apenas mudar o nome do tópico não resolveria esse problema. Ordenação causal não foi incluída neste incremento.

## Contrato

EmployeeStatusChangedEvent é um record de domínio independente de Spring, JPA e Kafka. Possui eventId, employeeId, occurredAt, previousStatus e newStatus. Rejeita campos obrigatórios nulos e estados iguais. A operação atualmente disponível produz apenas ACTIVE → INACTIVE; o evento não implementa reativação nem cria um novo caso de uso.

EmployeeStatusChangedMessage é o contrato JSON de transporte:

```json
{
  "schemaVersion": 1,
  "eventType": "EmployeeStatusChangedEvent",
  "eventId": "8b244421-cbf3-43df-b593-209c0bd23f39",
  "employeeId": "378a92dd-d4ce-4326-bb77-c1c0eac66926",
  "occurredAt": "2026-10-28T10:00:00Z",
  "previousStatus": "ACTIVE",
  "newStatus": "INACTIVE"
}
```

Os identificadores e o instante acima são exemplos. occurredAt vem do Clock no momento de registrar a mudança; não reutiliza createdAt do empregado nem representa necessariamente o instante exato de commit. Clock e Supplier<UUID> são injetados para testes determinísticos.

EmployeeEventMessage reúne apenas metadados comuns aos dois contratos e é uma interface sealed: os dois tipos concretos são explicitamente permitidos. Outbox e histórico recebem esses metadados sem exigir sobrecargas por tipo nas entidades JPA. A interface não pertence ao domínio e não é usada para desserialização polimórfica. Cada listener lê um record concreto e valida o contrato esperado; a mensagem não escolhe uma classe Java por header.

## Transação e concorrência

1. DeactivateEmployee lê Employee e sua versão e captura o status anterior.
2. Se não existe, retorna NOT_FOUND; se deactivate() retorna false, retorna ALREADY_INACTIVE. Nenhum desses caminhos acrescenta evento.
3. Se houve transição, saveVersioned salva e executa flush com a versão esperada.
4. Só após a gravação bem-sucedida, gera eventId, obtém o instante e chama EmployeeEventOutbox.append(EmployeeStatusChangedEvent).
5. JpaEmployeeEventOutbox exige MANDATORY e salva o evento na transação já iniciada por TransactionalEmployeeDeactivation.
6. O commit confirma mudança, incremento de versão e intenção de publicação juntos.

Um conflito otimista antes do append não deixa evento. O coordenador mantém uma releitura em nova transação; se a primeira requisição já confirmou a inativação, a segunda encontra INACTIVE e não produz outro evento. Um segundo conflito continua retornando 409.

Se a Outbox falhar, status e versão também sofrem rollback. Se a falha ocorrer depois de inserir o evento e antes do commit, ele também é desfeito. REQUIRES_NEW mantém a tentativa independente de eventual transação externa; MANDATORY da Outbox participa dessa tentativa interna, sem iniciar outra.

A API mantém 204 para transição ou repetição, 404 para id inexistente e 400 para id malformado. 204 não significa que o Kafka recebeu a mensagem ou que o histórico já foi processado.

## Publicação e consumo

KafkaOutboxDelivery recebe event_type da linha da Outbox e escolhe um dos dois tópicos configurados. Tipo desconhecido falha; o evento permanece pendente para diagnóstico/retry em vez de ser enviado para um destino padrão incorreto.

As propriedades são app.messaging.topic para criação e app.messaging.status-topic para mudança de status. Ambos usam employeeId como chave; o grupo configurável employee-history.v1 tem listeners distintos para os dois tópicos. O Compose do broker não precisou mudar; KafkaMessagingConfig declara os dois tópicos locais com uma partição e uma réplica.

EmployeeCreatedKafkaConsumer e EmployeeStatusChangedKafkaConsumer desserializam seus respectivos contratos e delegam a EmployeeEventHistoryHandler. Esse componente valida chave/employeeId, chama EmployeeHistoryWriter e registra logs com tipo, tópico, identificadores, partição e offset. Não expõe nome/e-mail no payload ou nesses logs.

EmployeeHistoryWriter mantém REQUIRES_NEW. A chave event_id garante um histórico por ocorrência lógica. O handler captura falha de integridade somente depois do rollback; consulta se o identificador já existe e propaga outras falhas. As entidades continuam usando Persistable para inserir UUIDs atribuídos sem transformar duplicatas em updates.

O container só confirma offset após processamento. Há retry por falha transitória e deduplicação por eventId. A produção Kafka idempotente não substitui essa deduplicação, pois um reenvio da Outbox após reinício pode ser uma nova publicação para o broker.

## Limites preservados

- Há um evento lógico por transição efetiva feita por este fluxo; o mesmo evento pode ser publicado e entregue várias vezes.
- Não existe ordem garantida entre os tópicos, mesmo usando a mesma chave. Retries da Outbox também podem mudar a ordem de publicação.
- O histórico apenas acrescenta registros; não modifica Employee nem exige histórico de criação anterior para aceitar inativação. Não é um mecanismo de reconstrução de estado por event sourcing.
- O estado atual do agregado vem da tabela employees. Consumir uma criação atrasada não reativa o empregado.
- Persistência de empregado e intenção é atômica; banco e Kafka não formam uma transação distribuída. Garantias dependem da recuperação e retenção descritas na decisão 006.
- Mantém um publicador de aplicação validado, lock e conexão durante envio, sem DLQ ou limpeza automática. Mensagens inválidas podem bloquear sua partição.
- Não há emissão retroativa de eventos de operações anteriores à implementação.
- H2 padrão continua volátil; PostgreSQL é necessário para durabilidade após encerrar o processo.

## Arquivos e responsabilidades

| Grupo | Arquivos criados/modificados |
|---|---|
| Domínio | EmployeeStatusChangedEvent |
| Aplicação | DeactivateEmployee, EmployeeEventOutbox |
| Configuração | ApplicationConfig, KafkaMessagingConfig, application.properties |
| Contratos | EmployeeEventMessage, EmployeeStatusChangedMessage, EmployeeCreatedMessage |
| Gravação/publicação | JpaEmployeeEventOutbox, OutboxPublishingTransaction, KafkaOutboxDelivery |
| Consumo | EmployeeStatusChangedKafkaConsumer, EmployeeCreatedKafkaConsumer, EmployeeEventHistoryHandler, EmployeeHistoryWriter |
| Persistência | OutboxEventEntity, EmployeeEventEntity |
| Testes | DeactivateEmployeeTest, EmployeeStatusChangedEventTest, EmployeeStatusOutboxIntegrationTest, EmployeeDeactivationApiIntegrationTest, EmployeeEventsKafkaIntegrationTest |
| Ajustes de regressão | CreateEmployeeTest, EmployeeOutboxIntegrationTest, OutboxPublishingIntegrationTest, EmployeeCreatedKafkaConsumerTest |
| Documentação | README, AI_USAGE, domínio, casos de uso e decisões 005, 006 e 007 |

EmployeeEventsKafkaIntegrationTest amplia e renomeia o antigo EmployeeCreatedKafkaIntegrationTest para cobrir os dois fluxos. O contrato JSON de EmployeeCreatedMessage continua com os mesmos campos e valores de tipo/versão.

## Verificação

- H2 + Kafka real: mvnw verify -Dkafka.tests=true, 63 testes, sem falhas, erros ou ignorados; jar executável gerado.
- PostgreSQL 16.15 + Kafka real: suíte com 63 testes, sem falhas, erros ou ignorados, usando a base exclusiva gestao_outbox_tests_20261027_01.
- Os testes unitários verificam versão transportada, evento com UUID/instante fixos, ausência de evento em repetição/id inexistente/conflito, propagação de falha na Outbox e validação de mudança real.
- A integração HTTP concorrente verifica duas respostas 204, versão incrementada uma vez e um evento lógico. Ela não força deterministicamente as duas requisições a lerem a mesma versão; o teste existente de versão obsoleta continua cobrindo esse conflito coordenado na persistência.
- Há falha simulada no repositório da Outbox e falha injetada no retorno do caso de uso após as gravações reais. Os testes confirmam rollback de status, versão e evento. Não representam uma interrupção real de processo ou rede.
- O teste Kafka usa POST e DELETE reais, processa a inativação antes de publicar a criação, força falha transitória no writer dos dois contratos e confirma o offset das duplicatas antes de verificar a unicidade do histórico.
- Nos testes controlados, o agendamento fica desabilitado e o publicador é chamado explicitamente; tópicos e grupo são exclusivos e removidos ao final. Testcontainers continua adiado.
- Smoke test do jar com PostgreSQL, dois tópicos exclusivos e agendamento habilitado: cadastro e dois DELETE retornaram 201/204/204, produziram dois eventos no histórico e mantiveram Employee INACTIVE com versão 1. Fixtures, tópicos e grupo foram removidos e o processo encerrado.
