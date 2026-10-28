# Roteiro de apresentação e defesa técnica

Roteiro sugerido de 15 minutos, ajustável ao tempo da avaliadora. Use os [diagramas](architecture.md) e demonstre as garantias com dados observáveis. O [checklist de entrega](delivery.md) separa implementação, evidências e pendências.

## Sequência sugerida

| Tempo | Tema | Mensagem principal |
|---|---|---|
| 1 min | Problema e escopo | Seis endpoints para gestão de pessoas e histórico assíncrono das duas mudanças de Employee |
| 2 min | Domínio e arquitetura | Entidades com identidade/comportamento, objetos de valor, serviço de domínio e portas independentes de frameworks |
| 3 min | Transações e eventos | Employee/Outbox no mesmo commit, publicação posterior e consumidor idempotente |
| 4 min | Demonstração | Cadastrar, consultar, rejeitar duplicidade, inativar duas vezes e conferir histórico |
| 2 min | Testes e CI | Testes de regras, banco real, concorrência e Kafka; Testcontainers reproduz o ambiente |
| 2 min | Limites e alternativas | Retry, ausência de ordem, custo do lock e evoluções proporcionais ao volume |
| 1 min | IA e responsabilidade | Mostrar uma sugestão revisada e sua consequência no código/teste |

Uma abertura possível: “Implementei os fluxos obrigatórios em uma aplicação com núcleo Java independente. As mudanças do colaborador e a intenção de evento são atômicas no banco; a entrega ao Kafka é posterior e admite reenvio, tratado pelo histórico idempotente.”

## Preparação do ambiente

Na raiz do repositório, em um terminal dedicado à demonstração, selecione um projeto Compose próprio e portas livres. Os comandos deste roteiro usam esse mesmo terminal:

```sh
export COMPOSE_PROJECT_NAME=gestao-apresentacao
export APP_PORT=18080
export POSTGRES_PORT=15432
export KAFKA_HOST_PORT=19094
export DEMO_API_URL=http://localhost:18080
docker compose up -d --build --wait
docker compose ps
curl --fail "$DEMO_API_URL/actuator/health"
```

O nome de projeto cria rede/volumes separados do desenvolvimento. Se alguma porta estiver ocupada, escolha outra; atualize DEMO_API_URL quando mudar APP_PORT. O nome isolado não torna as portas livres automaticamente. Mantenha as variáveis nos comandos posteriores ou exporte-as novamente em outro terminal.

Abra [Swagger UI da demonstração](http://localhost:18080/swagger-ui.html). Antes de apresentar, construa as imagens e baixe dependências para que a primeira execução não ocupe o tempo da avaliação. Se já houver dados de um ensaio anterior, use outro e-mail/nome identificável e registre os novos IDs; não apague volumes apenas para reiniciar a demonstração.

## Demonstrar os seis endpoints

Como alternativa aos comandos curl abaixo, use a [collection Postman](../postman/README.md). Ela captura IDs automaticamente e executa o fluxo em ordem; no environment local, ajuste baseUrl para http://localhost:18080 quando usar este ambiente de apresentação. A consulta SQL e a explicação das garantias assíncronas continuam necessárias para demonstrar histórico.

### 1. Cadastrar departamento e cargo

```sh
curl -i "$DEMO_API_URL/departments" -H 'Content-Type: application/json' \
  -d '{"name":"RH Apresentação","costCenterCode":"CC-DEMO"}'
curl -i "$DEMO_API_URL/positions" -H 'Content-Type: application/json' \
  -d '{"title":"Engenheiro de Software","level":"Sênior"}'
```

Os dois retornam 201 e Location. Copie os IDs dos corpos para estas variáveis; os textos abaixo são placeholders, não UUIDs válidos:

```sh
export DEMO_DEPARTMENT_ID='UUID-RETORNADO-DO-DEPARTAMENTO'
export DEMO_POSITION_ID='UUID-RETORNADO-DO-CARGO'
```

### 2. Cadastrar colaborador

```sh
curl -i "$DEMO_API_URL/employees" -H 'Content-Type: application/json' --data-binary @- <<JSON
{
  "name": "Ana Demonstração",
  "email": " ANA.DEMO@EXAMPLE.COM ",
  "departmentId": "$DEMO_DEPARTMENT_ID",
  "positionId": "$DEMO_POSITION_ID"
}
JSON
```

Espere 201, status ACTIVE, e-mail ana.demo@example.com e UUID/data definidos pelo servidor. Copie o ID retornado:

```sh
export DEMO_EMPLOYEE_ID='UUID-RETORNADO-DO-COLABORADOR'
```

Explique que 201 confirma o commit de Employee e Outbox. Não conclua que o consumidor já terminou.

### 3. Consultar e verificar paginação

```sh
curl --get "$DEMO_API_URL/employees" \
  --data-urlencode 'name=Ana Demonstração' --data-urlencode 'status=ACTIVE' \
  --data-urlencode 'page=0' --data-urlencode 'size=20'
curl --get "$DEMO_API_URL/employees/search" \
  --data-urlencode 'department=RH Apresentação'
```

Localize o ID recém-criado no content. Explique os cinco campos da página, ordenação createdAt/id e diferença entre nome parcial e departamento exato. Departamento homônimo pode retornar outros colaboradores; não pressuponha unicidade do nome.

### 4. Demonstrar rejeições

Repita o comando de cadastro de Employee, com o mesmo e-mail: deve retornar 409/EMAIL_ALREADY_EXISTS. O banco garante a unicidade sob concorrência, mesmo após inativação.

Para demonstrar 422, use e-mail diferente e um departamento inexistente:

```sh
curl -i "$DEMO_API_URL/employees" -H 'Content-Type: application/json' --data-binary @- <<JSON
{
  "name": "Referência inválida",
  "email": "referencia.invalida.demo@example.com",
  "departmentId": "00000000-0000-4000-8000-000000000099",
  "positionId": "$DEMO_POSITION_ID"
}
JSON
```

Confirme que o ID de exemplo não foi cadastrado no ambiente. Não confunda 422 de referência bem formada/inexistente com 400 de UUID malformado.

### 5. Inativar duas vezes

```sh
curl -i -X DELETE "$DEMO_API_URL/employees/$DEMO_EMPLOYEE_ID"
curl -i -X DELETE "$DEMO_API_URL/employees/$DEMO_EMPLOYEE_ID"
curl --get "$DEMO_API_URL/employees" \
  --data-urlencode 'name=Ana Demonstração' --data-urlencode 'status=INACTIVE'
```

As duas chamadas DELETE retornam 204, mas só a transição efetiva gera EmployeeStatusChangedEvent. O cadastro é preservado. Não simule concorrência apenas clicando rapidamente: mostre o teste coordenado de versão se a avaliadora perguntar.

### 6. Conferir Outbox e histórico

```sh
docker compose exec -T postgres psql -U gestao -d gestao_pessoas \
  -v employee_id="$DEMO_EMPLOYEE_ID" <<'SQL'
SELECT e.id, e.status, e.version, o.event_id, o.event_type,
       o.published_at, h.processed_at
FROM employees e
LEFT JOIN outbox_events o ON o.employee_id = e.id
LEFT JOIN employee_events h ON h.event_id = o.event_id
WHERE e.id = :'employee_id'::uuid
ORDER BY o.occurred_at, o.event_id;
SQL
```

Depois do processamento assíncrono, espere duas linhas para esse colaborador: criação e mudança de status, com published_at e processed_at preenchidos; status INACTIVE e versão 1. Repita a consulta se ainda houver pendência. Repetir o DELETE não deve acrescentar uma terceira ocorrência lógica. A ordenação desta consulta é para apresentação; não prova a ordem de entrega entre tópicos.

Para ver os logs customizados:

```sh
docker compose logs app --since 5m
```

Procure outbox_published e employee_event_consumed com os IDs da demonstração. Em falha, confira também outbox_publish_failed e os campos attempts/available_at/last_error da Outbox. Kafka parado pode deixar o cadastro confirmado e evento pendente; 201/204 não são confirmação de consumo.

Ao encerrar, `docker compose stop` preserva o ambiente e seus dados. `down` também preserva volumes; `down -v` apaga os dados daquele projeto. Não execute a remoção de volumes sobre o projeto de desenvolvimento.

## Evidências para apresentar

Use uma execução verde da [CI](https://github.com/hodestito/gestao-pessoas/actions) e abra o artefato verificacao-java-postgres-kafka. Em site/jacoco/index.html, mostre linhas e ramos separadamente. A medição de referência é 87 testes completos, 99,07% de linhas e 95,69% de ramos; indique a revisão executada e não prometa ausência de defeitos.

Se houver tempo e Docker estiver acessível, execute `./mvnw -Ptestcontainers clean verify`. Se não houver, mostre o resultado já produzido e o comando de reprodução. `./mvnw clean verify` usa H2 e ignora dois testes Kafka; não apresente esse modo como validação do broker real.

| O que provar | Arquivo para abrir |
|---|---|
| Regra e transição de estado | domain/employee/Employee.java e EmployeeRegistrationPolicy.java |
| Fluxo de aplicação e intenção de evento | application/CreateEmployee.java e DeactivateEmployee.java |
| Transação e nova tentativa | infrastructure/config/TransactionalEmployeeRegistration.java e EmployeeDeactivationCoordinator.java |
| Mapeamento e versão | infrastructure/persistence/EmployeeEntity.java e EmployeePersistenceAdapter.java |
| Envio confirmado e reenvio | infrastructure/messaging/OutboxPublishingTransaction.java |
| Deduplicação após rollback | infrastructure/messaging/EmployeeEventHistoryHandler.java e EmployeeHistoryWriter.java |
| Infraestrutura descartável | src/test/java/br/com/desafio/gestaopessoas/support/IntegrationContainersConfiguration.java |

Os seis primeiros caminhos são relativos a src/main/java/br/com/desafio/gestaopessoas/. Os testes correspondentes estão na [matriz de evidências](delivery.md).

## Perguntas prováveis e pontos da resposta

| Pergunta | Pontos para sustentar a resposta |
|---|---|
| Onde está o DDD? | Linguagem compartilhada, agregados por ID, valores válidos por construção, transição em Employee e política de admissão; não apenas nomes de pacotes |
| Por que separar domínio e JPA? | Evita que persistência dite o modelo; custo explícito de entidades/mapeamentos adicionais |
| Por que não usar record em toda entidade? | Record compara componentes por padrão; as entidades atuais usam identidade por ID. Records permanecem adequados para valores/eventos imutáveis |
| Por que Clock e Supplier de UUID? | Testes controlam instante/identidade sem depender de relógio real ou aleatoriedade; produção injeta implementações reais |
| Quem cria os objetos sem @Service no núcleo? | ApplicationConfig declara beans e o Spring injeta dependências pelo construtor |
| A consulta de e-mail garante unicidade? | Não sob concorrência; consulta antecipa a resposta e constraint garante a regra |
| Flush é commit? | Flush envia SQL e antecipa violações; a transação ainda pode sofrer rollback antes do commit |
| @Version bloqueia desde a leitura? | Não. Atualização com versão antiga falha; a tentativa seguinte relê em nova transação |
| Por que Outbox e não só AFTER_COMMIT? | O callback deixa janela entre commit e registro durável da intenção; Outbox grava ambos atomicamente no banco |
| E se Kafka recebeu e a marcação falhou? | Reenvio com mesmo eventId; PK do histórico e handler tornam o processamento idempotente |
| Por que dois tópicos? | Preservação do contrato de criação e separação de contratos, assumindo ausência de ordem entre eles |
| Por que o offset vem depois do histórico? | Confirmar antes poderia perder processamento em uma falha; repetir é preferível e a deduplicação trata a repetição |
| Por que não exactly-once? | Banco e Kafka não compartilham transação atômica; idempotência do histórico é uma garantia mais restrita |
| Testcontainers substitui Flyway ou fixtures? | Não: cria serviços; Flyway aplica schema e fixtures controlam os dados por teste |
| O que falta para produção? | Citar limites reais de autenticação, operação de mensagens inválidas, retenção, observabilidade, carga e disponibilidade; não apresentar sugestões como entregues |

Para mostrar o uso consciente de IA, escolha uma mudança real em [AI_USAGE.md](../AI_USAGE.md): JDBC substituído por JPA após sua revisão, Spring Events revisto para Kafka, ou retirada de Spring Data do núcleo. Explique o motivo e a evidência; não atribua à IA decisões que o candidato tomou.
