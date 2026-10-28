# Gestão de pessoas — desafio técnico

Projeto em construção para o desafio técnico. Estão implementados os seis endpoints. O cadastro registra EmployeeCreatedEvent em Outbox, com publicação Kafka e consumidor de histórico. O evento de inativação ainda está pendente.

Employee contempla criação ativa, e-mail normalizado e inativação no domínio. O cadastro persiste empregado e evento na mesma transação. A inativação preserva o registro e usa controle otimista de versão.

## Documentação e evolução

- [Domínio](docs/domain.md): linguagem compartilhada, regras confirmadas, propostas e situação da modelagem DDD.
- [Casos de uso](docs/use-cases.md): os seis fluxos, contratos existentes/propostos e cenários de aceitação.
- [Decisão 001](docs/decisions/001-employee-e-kafka.md): identidade de Employee, regras aprovadas e direção para Kafka.
- [Decisão 002](docs/decisions/002-cadastro-employee.md): transação, persistência e respostas do cadastro.
- [Decisão 003](docs/decisions/003-listagem-colaboradores.md): Page, filtros e limites da listagem.
- [Decisão 004](docs/decisions/004-busca-por-departamento.md): busca por departamento e critérios adotados para revisão.
- [Decisão 005](docs/decisions/005-inativacao-version.md): inativação, concorrência e transações.
- [Decisão 006](docs/decisions/006-outbox-cadastro.md): Outbox, publicação Kafka, idempotência, limites e arquivos envolvidos.
- [Uso de IA](AI_USAGE.md): participação da IA e padrão dos commits identificados com `[IA]`.

Os documentos distinguem requisitos do enunciado, definições do candidato, propostas e comportamento implementado. O Git registra a evolução real; os documentos apresentam o entendimento atual. A arquitetura detalhada e outros registros de decisão serão acrescentados conforme discutirmos essas decisões.

## Base escolhida

- Java 21 e Spring Boot 4.1.2
- Maven Wrapper, Spring Web, JPA para agregados, Outbox e histórico, Flyway, Kafka, validação, Actuator, H2 e driver PostgreSQL
- H2 é o banco usado por padrão; PostgreSQL pode ser selecionado pelo perfil `postgres`. O Flyway cria as tabelas de departamentos, cargos e colaboradores.

## Primeiro incremento

O domínio concentra validações e não depende do Spring ou do JPA. Os casos de uso chamam interfaces de repositório; adaptadores JPA fazem a persistência. A API valida as entradas e retorna `201 Created` com o identificador gerado.

Exemplos com a aplicação em execução na porta 8080:

```sh
curl -X POST http://localhost:8080/departments -H 'Content-Type: application/json' -d '{"name":"RH","costCenterCode":"CC-01"}'
curl -X POST http://localhost:8080/positions -H 'Content-Type: application/json' -d '{"title":"Engenheiro","level":"Sênior"}'
curl -X POST http://localhost:8080/employees -H 'Content-Type: application/json' -d '{"name":"Hugo","email":"hugo@example.com","departmentId":"UUID-DO-DEPARTAMENTO","positionId":"UUID-DO-CARGO"}'
curl 'http://localhost:8080/employees?name=hugo&status=ACTIVE&page=0&size=20'
curl 'http://localhost:8080/employees/search?department=RH&page=0&size=20'
curl -X DELETE 'http://localhost:8080/employees/UUID-DO-COLABORADOR'
```

Substitua os identificadores pelos UUIDs retornados pelos dois primeiros cadastros. E-mail duplicado retorna 409; departamento ou cargo inexistente retorna 422. O cadastro grava EmployeeCreatedEvent na Outbox; 201 confirma o cadastro e a intenção de publicação, sem esperar o histórico assíncrono.

A listagem aceita filtros opcionais `name` e `status`, páginas iniciadas em zero e tamanho de 1 a 100 (padrão 20). Busca por nome é parcial e insensível a caixa, sem garantia de ignorar acentos. A resposta contém `content`, `page`, `size`, `totalElements` e `totalPages`. Consulte [casos de uso](docs/use-cases.md) para os limites.

A busca por departamento usa correspondência exata sem diferenciar maiúsculas/minúsculas, inclui ativos e inativos e retorna colaboradores de todos os departamentos com o mesmo nome. Department ausente ou vazio resulta em 400; nenhum resultado retorna 200 com conteúdo vazio.

A inativação retorna 204 quando realizada ou quando o colaborador já estava inativo; ID inexistente retorna 404. Um segundo conflito de versão após uma nova tentativa retorna 409. Substitua o identificador pelo UUID do colaborador. A operação ainda não emite o evento obrigatório.

## Primeira verificação

Com um JDK 21 disponível, execute `./mvnw verify`. O Maven Wrapper baixa a versão de Maven indicada no projeto e não exige uma instalação global de Maven.

Neste Mac, o JDK 21 está disponível para os projetos do usuário na pasta `~/Library/Java/JavaVirtualMachines/temurin-21.jdk`. Para selecioná-lo no terminal:

```sh
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
./mvnw verify
```

No IntelliJ, selecione a pasta `~/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home` como Project SDK e use o Maven Wrapper ao importar o `pom.xml`. O JDK instalado no Mac não faz parte da entrega.

## PostgreSQL local

Com o Docker Desktop em execução, use `docker compose up -d postgres` nesta pasta. A conexão é limitada ao próprio computador, na porta 5432, com banco `gestao_pessoas`, usuário `gestao` e senha local `gestao_local`. Os dados ficam em um volume Docker. Essas credenciais são apenas para desenvolvimento.

Para iniciar a aplicação usando esse banco: `./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres`. Sem esse perfil, a base inicial usa H2 em memória. Para parar o banco sem apagar os dados, use `docker compose stop`.

## Eventos de cadastro com Outbox e Kafka

Inicie os serviços e a aplicação:

```sh
docker compose up -d --wait postgres kafka
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres,kafka
```

O Compose usa Kafka 4.1.0 em KRaft, com um broker e volume persistente. A aplicação no Mac conecta em localhost:9092; uma aplicação na rede do Compose usaria kafka:19092, via KAFKA_BOOTSTRAP_SERVERS. O tópico employee-created.v1 tem uma partição e uma réplica, adequado ao ambiente local.

O cadastro grava empregado e evento na mesma transação PostgreSQL. A cada segundo, um publicador seleciona até 20 pendentes, envia cada um com employeeId como chave e aguarda confirmação do Kafka antes de marcar published_at. Falhas aguardam cinco segundos antes de nova tentativa. eventId permanece igual nos reenvios.

O consumidor employee-history.v1 grava employee_events e produz logs com identificadores, sem nome/e-mail. A chave primária event_id impede duplicatas. O offset é confirmado após processamento; falhas são repetidas. Isso oferece entrega pelo menos uma vez com histórico idempotente, sob disponibilidade e retenção dos dados; não oferece exactly-once entre banco e Kafka.

Sem o perfil kafka, os eventos continuam na Outbox, sem publicação. H2 em memória é o padrão e perde empregado e Outbox ao encerrar o processo; use PostgreSQL para durabilidade. Cadastros anteriores à V4 não recebem eventos retroativos.

Consulte o estado no PostgreSQL:

```sql
SELECT event_id, employee_id, attempts, published_at, available_at, last_error
FROM outbox_events ORDER BY occurred_at;
SELECT event_id, employee_id, event_type, occurred_at, processed_at
FROM employee_events ORDER BY processed_at;
```

O desenho mantém uma conexão e um bloqueio de linha durante cada envio, com espera limitada; prioriza simplicidade e baixo volume. O publicador foi validado com uma instância. Não há garantia de ordenação entre eventos, limpeza automática de registros ou DLQ. Uma mensagem inválida bloqueia sua partição até intervenção; os limites e alternativas estão na decisão 006. O evento EmployeeStatusChangedEvent será tratado no próximo incremento.

## Testes de Outbox e Kafka

`./mvnw test` executa a suíte sem exigir Kafka; o teste do broker real fica explicitamente ignorado. Para incluir esse teste com H2:

```sh
docker compose up -d --wait kafka
./mvnw test -Dkafka.tests=true
```

Para validar também PostgreSQL, crie uma base exclusiva de testes (comando de criação executado apenas uma vez):

```sh
docker compose up -d --wait postgres kafka
docker compose exec -T postgres createdb -U gestao gestao_pessoas_test
./mvnw test -Dkafka.tests=true -Dspring.profiles.active=postgres \
  -Dspring.datasource.url=jdbc:postgresql://localhost:5432/gestao_pessoas_test \
  -Dspring.datasource.username=gestao -Dspring.datasource.password=gestao_local
```

O teste Kafka cria e remove seu próprio tópico e grupo. O agendamento é desabilitado nos testes controlados de falhas; o publicador é chamado explicitamente e o consumidor é real. Não use uma base de produção ou com outros publicadores ativos. Testcontainers permanece adiado por decisão do responsável técnico.

## Próximos incrementos

1. Estender Outbox e histórico para o evento de inativação, preservando idempotência e controle de versão.
2. Revisar regras de domínio e casos de uso com o responsável técnico.
3. Revisar operação da mensageria, retenção e tratamento de mensagens inválidas conforme o escopo.
4. Acrescentar testes de integração, Docker, documentação da API e registro do uso de IA.

Dockerfile, documentação Swagger, CI e medição de cobertura continuam pendentes.
