# Gestão de pessoas — desafio técnico

Projeto em construção para o desafio técnico. Os seis endpoints, os eventos com Outbox/Kafka, a política de admissão, a revisão de paginação/identidade, a cobertura, a documentação OpenAPI, Docker/Compose e CI estão integrados à main. Esta branch acrescenta Testcontainers aos testes e à CI, para revisão.

Employee contempla criação ativa, e-mail normalizado e inativação no domínio. Cadastro e inativação persistem empregado e seu evento na mesma transação. A inativação preserva o registro, usa controle otimista de versão e só registra evento se houver mudança efetiva.

## Documentação e evolução

- [Domínio](docs/domain.md): linguagem compartilhada, regras confirmadas, propostas e situação da modelagem DDD.
- [Casos de uso](docs/use-cases.md): os seis fluxos, contratos existentes/propostos e cenários de aceitação.
- [Decisão 001](docs/decisions/001-employee-e-kafka.md): identidade de Employee, regras aprovadas e direção para Kafka.
- [Decisão 002](docs/decisions/002-cadastro-employee.md): transação, persistência e respostas do cadastro.
- [Decisão 003](docs/decisions/003-listagem-colaboradores.md): paginação, filtros e limites da listagem.
- [Decisão 004](docs/decisions/004-busca-por-departamento.md): busca por departamento e critérios adotados para revisão.
- [Decisão 005](docs/decisions/005-inativacao-version.md): inativação, concorrência e transações.
- [Decisão 006](docs/decisions/006-outbox-cadastro.md): Outbox, publicação Kafka, idempotência, limites e arquivos envolvidos.
- [Decisão 007](docs/decisions/007-evento-inativacao.md): evento de inativação, dois tópicos e histórico compartilhado.
- [Decisão 008](docs/decisions/008-servico-dominio-cadastro.md): política de admissão, consultas do domínio e divisão de responsabilidades.
- [Decisão 009](docs/decisions/009-nucleo-independente-e-identidade.md): paginação própria e identidade das entidades por ID.
- [Decisão 010](docs/decisions/010-cobertura-e-caminhos-criticos.md): medição de cobertura, lacunas priorizadas e resultados.
- [Decisão 011](docs/decisions/011-openapi-swagger.md): documentação HTTP, alternativas e limites do contrato gerado.
- [Decisão 012](docs/decisions/012-docker-compose.md): imagem da aplicação, rede, inicialização e validação do Compose.
- [Decisão 013](docs/decisions/013-integracao-continua.md): workflow, ambiente de testes, artefatos e limites da automação.
- [Decisão 014](docs/decisions/014-testcontainers.md): serviços descartáveis de teste, conexões dinâmicas e ciclo de vida.
- [Uso de IA](AI_USAGE.md): participação da IA e padrão dos commits identificados com `[IA]`.

Os documentos distinguem requisitos do enunciado, definições do candidato, propostas e comportamento implementado. O Git registra a evolução real; os documentos apresentam o entendimento atual. A arquitetura detalhada e outros registros de decisão serão acrescentados conforme discutirmos essas decisões.

## Base escolhida

- Java 21 e Spring Boot 4.1.2
- Maven Wrapper, Spring Web, JPA para agregados, Outbox e histórico, Flyway, Kafka, validação, Actuator, H2 e driver PostgreSQL
- H2 é o banco usado por padrão; PostgreSQL pode ser selecionado pelo perfil `postgres`. O Flyway cria as tabelas de departamentos, cargos e colaboradores.

## Execução completa com Docker Compose

Requer Docker Engine e Compose v2 ou posterior (por exemplo, Docker Desktop em execução). Não exige JDK ou Maven instalados no computador. Na raiz do repositório:

```sh
docker compose up -d --build --wait
docker compose ps
curl --fail http://localhost:8080/actuator/health
```

Esse comando compila a aplicação em uma etapa com JDK 21 e Maven Wrapper, e a executa em outra com JRE 21, usando um usuário sem privilégios. O primeiro build precisa de internet para imagens e dependências; o cache do BuildKit reaproveita downloads posteriores. O build da imagem usa -DskipTests para empacotar; os comandos de testes estão nas seções próprias e devem ser executados antes da integração da branch.

O Compose ativa os perfis postgres,kafka na aplicação, cria o schema com Flyway e mantém os dados em volumes. PostgreSQL e Kafka devem ficar saudáveis antes de iniciar a aplicação; o healthcheck dela consulta Actuator. Acesse [Swagger UI](http://localhost:8080/swagger-ui.html) e use os exemplos HTTP deste README. As portas publicadas ficam limitadas ao próprio computador: aplicação 8080, PostgreSQL 5432 e Kafka 9092. Usuário gestao e senha gestao_local são credenciais apenas do ambiente local.

Entre containers, a aplicação usa postgres:5432 e kafka:19092. localhost dentro do container aponta para ele próprio. A porta Kafka 9092 anuncia o endereço para clientes no Mac; o listener 19092 anuncia o nome kafka para clientes da rede Compose.

Para consultar logs e parar sem apagar os dados:

```sh
docker compose logs -f app
docker compose stop
```

docker compose down remove containers e rede, mas mantém volumes; não acrescente -v se quiser preservar os dados. Alterações nos fontes exigem novo build. Para executar a aplicação no IntelliJ/Maven, suba apenas as dependências com `docker compose up -d --wait postgres kafka` e pare app, se estiver rodando, para liberar a porta 8080 e evitar dois publicadores sobre a mesma base.

APP_PORT, POSTGRES_PORT e KAFKA_HOST_PORT permitem trocar somente as portas do computador. Ao usar outro nome de projeto com -p, o Compose cria rede e volumes independentes. Exemplo usado na validação:

```sh
APP_PORT=18080 POSTGRES_PORT=15432 KAFKA_HOST_PORT=19094 \
  docker compose -p gestao-docker-smoke up -d --build --wait
```

Nesse caso, use http://localhost:18080 para a API. Mantenha as mesmas variáveis e o mesmo -p nos comandos posteriores desse ambiente. As portas internas continuam 8080/5432/19092. Os healthchecks verificam disponibilidade naquele momento; não garantem disponibilidade futura nem entrega imediata dos eventos.

## Primeiro incremento

O domínio concentra validações e não depende do Spring ou do JPA. EmployeeRegistrationPolicy decide se departamento/cargo existem e se o e-mail está disponível, consultando a porta EmployeeRegistrationRequirements. O adaptador JPA fornece essas informações. CreateEmployee coordena validação, criação/persistência e Outbox dentro da transação. A API valida as entradas e retorna `201 Created` com o identificador gerado.

A política é uma classe Java sem @Service; o Spring a instancia na configuração. Employee protege seus valores e transições; a política reúne condições que envolvem outros registros; o caso de uso coordena a operação; constraints de unicidade e chaves estrangeiras garantem integridade sob concorrência.

Domínio e aplicação usam apenas tipos próprios e da biblioteca Java. EmployeePagination valida os parâmetros de consulta e PageResult representa o resultado; somente a infraestrutura converte esses tipos para PageRequest/Page do Spring Data e aplica a ordenação contratada. O JSON paginado mantém content, page, size, totalElements e totalPages.

Employee, Department e Position são entidades com igualdade e hashCode por ID, dentro do próprio tipo. Department e Position são classes finais com dados imutáveis e as mesmas validações/acessores anteriores. Email e CostCenterCode continuam sendo objetos de valor, comparados por seus valores.

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

A inativação retorna 204 quando realizada ou quando o colaborador já estava inativo; ID inexistente retorna 404. Um segundo conflito de versão após uma nova tentativa retorna 409. Substitua o identificador pelo UUID do colaborador. Somente a transição efetiva registra EmployeeStatusChangedEvent, com previousStatus=ACTIVE e newStatus=INACTIVE. A resposta 204 não espera a publicação nem o histórico.

## Documentação interativa da API

Com a aplicação em execução, acesse [Swagger UI](http://localhost:8080/swagger-ui.html). O contrato é gerado em [OpenAPI JSON](http://localhost:8080/v3/api-docs) e [OpenAPI YAML](http://localhost:8080/v3/api-docs.yaml). A interface usa recursos locais, sem depender de CDN. Ela permite executar requisições reais no ambiente conectado; cadastros e inativações alteram seus dados.

springdoc-openapi 3.2.0 lê os controllers e DTOs HTTP. OpenAPI é o formato do contrato; Swagger UI apresenta esse contrato e permite explorá-lo. As anotações documentam filtros, paginação, referências por UUID, exemplos, respostas e a confirmação assíncrona dos eventos. Não foram acrescentadas dependências de documentação ao domínio ou à aplicação.

O contrato preserva os dois formatos atuais de erro: ApiError (`code`, `message`) nas falhas tratadas da aplicação e FrameworkError (`timestamp`, `status`, `error`, `path`) em falhas padrão do Spring. Onde ambos são possíveis, usa oneOf. O DELETE de um ID inexistente retorna 404 sem corpo. As anotações descrevem o comportamento e não substituem validações nem testes.

Para experimentar o cadastro, crie departamento e cargo primeiro e copie seus IDs para o corpo de Employee. Os UUIDs de exemplo da documentação são ilustrativos. 201/204 confirmam a operação e a intenção na Outbox; não garantem que o histórico Kafka já tenha sido processado. Os contratos dos tópicos permanecem nas decisões 006 e 007; OpenAPI descreve somente HTTP.

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

## Eventos com Outbox e Kafka

Inicie os serviços e a aplicação:

```sh
docker compose up -d --wait postgres kafka
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres,kafka
```

O Compose usa Kafka 4.1.0 em KRaft, com um broker e volume persistente. A aplicação no Mac conecta em localhost:9092; uma aplicação na rede do Compose usaria kafka:19092, via KAFKA_BOOTSTRAP_SERVERS. Os tópicos employee-created.v1 e employee-status-changed.v1 possuem uma partição e uma réplica cada, para o ambiente local. As propriedades app.messaging.topic e app.messaging.status-topic permitem configurar os nomes; o responsável técnico escolheu dois tópicos preservando o contrato existente.

Cadastro e inativação gravam mudança e evento na mesma transação PostgreSQL. Uma falha na Outbox desfaz também a mudança de estado e o incremento de versão. A cada segundo, um publicador seleciona até 20 pendentes, escolhe o tópico por event_type, envia com employeeId como chave e aguarda confirmação do Kafka antes de marcar published_at. Falhas aguardam cinco segundos antes de nova tentativa. eventId permanece igual nos reenvios.

Os consumidores dos dois tópicos usam o grupo employee-history.v1, compartilham a gravação em employee_events e produzem logs com tipo e identificadores, sem nome/e-mail. A chave primária event_id impede duplicatas. O offset é confirmado após processamento; falhas são repetidas. Isso oferece entrega pelo menos uma vez com histórico idempotente, sob disponibilidade e retenção dos dados; não oferece exactly-once entre banco e Kafka.

Não há garantia de ordem entre os tópicos, nem entre eventos sujeitos a retry no publicador. O histórico aceita inativação antes da criação e apenas acrescenta registros; ele não reaplica eventos para modificar Employee. O payload de inativação registra os estados anterior/novo e o instante da mudança, não a data de criação do empregado.

Sem o perfil kafka, os eventos continuam na Outbox, sem publicação. H2 em memória é o padrão e perde empregado e Outbox ao encerrar o processo; use PostgreSQL para durabilidade. Cadastros anteriores à V4 não recebem eventos retroativos.

Consulte o estado no PostgreSQL:

```sql
SELECT event_id, employee_id, attempts, published_at, available_at, last_error
FROM outbox_events ORDER BY occurred_at;
SELECT event_id, employee_id, event_type, occurred_at, processed_at
FROM employee_events ORDER BY processed_at;
```

O desenho mantém uma conexão e um bloqueio de linha durante cada envio, com espera limitada; prioriza simplicidade e baixo volume. O publicador foi validado com uma instância. Não há garantia de ordenação entre eventos, limpeza automática de registros ou DLQ. Uma mensagem inválida bloqueia sua partição até intervenção; os limites e alternativas estão nas decisões 006 e 007. Não há emissão retroativa de eventos de operações anteriores à implementação.

## Testes de Outbox e Kafka

`./mvnw test` preserva o modo sem Docker: usa H2 e ignora explicitamente os dois testes com broker real. Testes unitários de mensageria e testes da Outbox continuam executando.

Para a suíte completa, basta ter um runtime Docker acessível e executar:

```sh
./mvnw -Ptestcontainers clean verify
```

O perfil Maven habilita integration.containers e kafka.tests na JVM de testes. Testcontainers 2.0.5, gerenciado pelo Spring Boot, cria PostgreSQL 16 e Kafka 4.1.0 em portas livres; não é necessário subir Compose, criar banco ou informar URL/senha. O primeiro uso pode baixar imagens e precisa de internet. Sem Docker acessível, o modo completo falha; não ignora silenciosamente a integração.

IntegrationContainersConfiguration é exclusiva de testes. @ServiceConnection fornece JDBC/Flyway/Kafka ao Spring, sobrescrevendo endereços externos. Cada contexto Spring recebe seu PostgreSQL; contextos reaproveitados pelo cache compartilham o mesmo container, e as fixtures continuam responsáveis por remover seus registros. Apenas o contexto com perfil Spring kafka cria o broker. Os containers são encerrados pelo ciclo de vida do contexto e removidos pelo mecanismo de limpeza do Testcontainers; não usam os volumes ou portas fixas do Compose de desenvolvimento.

O perfil Maven testcontainers configura a execução dos testes; não é um perfil Spring da aplicação. A execução normal continua usando H2 por padrão e PostgreSQL/Kafka quando selecionados pelos perfis já documentados.

Os testes Kafka verificam criação, inativação repetida, consumo em ordem inversa, falhas transitórias e duplicatas com confirmação de offset. O agendamento é desabilitado nos testes controlados de falhas; o publicador é chamado explicitamente e os consumidores são reais. Eles consultam a configuração efetiva do KafkaAdmin, sem presumir porta 9092.

O modo anterior com serviços externos continua possível ao omitir -Ptestcontainers e informar kafka.tests, perfil postgres e datasource de uma base exclusiva. A CI e a reprodução recomendada agora usam containers descartáveis.

## Cobertura de testes

JaCoCo 0.8.16 acompanha a execução na JVM de testes e gera relatórios no verify. Para reproduzir a medição completa com PostgreSQL e Kafka reais e descartáveis:

```sh
./mvnw -Ptestcontainers clean verify
```

O relatório HTML fica em target/site/jacoco/index.html; o XML, em target/site/jacoco/jacoco.xml; os dados de execução, em target/jacoco.exec. No Mac, abra o HTML com `open target/site/jacoco/index.html`. target/ é gerado localmente e não é versionado. Cada execução substitui os dados anteriores; clean remove relatórios antigos. Executar apenas test não atualiza o HTML/XML, pois o relatório está ligado ao verify.

Medição de 28/10/2026, confirmada também com Testcontainers/PostgreSQL/Kafka, com 87 testes sem falhas ou ignorados:

| Escopo | Linhas | Ramos |
|---|---:|---:|
| Total | 99,07% (426/430) | 95,69% (111/116) |
| Domínio | 100% (92/92) | 90,48% (38/42) |
| Aplicação | 100% (60/60) | 100% (30/30) |
| Infraestrutura | 99,12% (225/227) | 96,55% (28/29) |
| Interfaces | 100% (48/48) | 100% (15/15) |

Os números foram iguais nas execuções anteriores com H2/Kafka e PostgreSQL/Kafka externos e na execução com Testcontainers, sem somar seus dados. O relatório inclui todas as classes de produção analisáveis, sem exclusões manuais. A medição inicial, com 74 testes, era de 94,90% de linhas e 81,03% de ramos.

Os testes adicionais verificam listagem HTTP sem filtros/nome em branco, contratos Kafka inválidos sem escrita de histórico, tipo desconhecido sem envio ao tópico errado, processamento do lote após um evento adiado, interrupção do poller e tradução de falhas conhecidas/desconhecidas da persistência. Os quatro testes de OpenAPI conferem o contrato gerado, referências, paginação, erros e recursos locais da UI. A medição histórica do PR #8 permanece na decisão 010.

Cobertura mostra código executado, não comprova todas as regras, concorrências ou falhas de infraestrutura. A meta sugerida de 80% serve como referência; não há gate percentual automático configurado. Sem o perfil testcontainers ou a propriedade kafka.tests, os dois testes Kafka reais ficam ignorados e os percentuais podem mudar. As lacunas e limites estão na decisão 010.

## Integração contínua

O workflow [CI](.github/workflows/ci.yml) executa em PRs destinados à main e em pushes na main. Um runner Ubuntu 24.04 configura Temurin 21 e cache Maven; a suíte inicia PostgreSQL/Kafka com Testcontainers em portas dinâmicas. O Compose permanece usado para construir a imagem da aplicação.

O Maven Wrapper executa -Ptestcontainers clean verify, incluindo os dois testes de broker real. Depois dos testes, o workflow constrói a imagem Docker. Falhas nos testes ou na construção reprovam o job. A cobertura é medida pelo JaCoCo; não há bloqueio por percentual mínimo.

Na aba Actions do repositório, abra a execução CI para consultar etapas e resultados. O artefato verificacao-java-postgres-kafka contém relatórios Surefire (incluindo saída dos testes) e cobertura HTML/XML/CSV, retidos por sete dias. Após baixar e extrair, abra site/jacoco/index.html; como os caminhos publicados estão sob target/, esse prefixo pode ser removido do arquivo extraído. Relatórios de cobertura só existirão se o Maven alcançar a fase verify.

Os serviços são descartados por Testcontainers ao final. Uma nova atualização do mesmo PR cancela a execução anterior para evitar validar uma revisão ultrapassada. O workflow solicita apenas leitura do conteúdo e fixa as actions por SHA. Não exige secrets de infraestrutura nem publica imagem em registry. O status do workflow informa o resultado; impedir merges com falha exige configurar proteção da branch separadamente, o que não foi alterado neste incremento.

A [primeira execução no GitHub Actions](https://github.com/hodestito/gestao-pessoas/actions/runs/40707903180), revisão 74d3d9c, passou com 87 testes, zero falhas, erros ou ignorados, e construção Docker concluída. Os relatórios baixados do runner registraram 99,07% de linhas (426/430) e 95,69% de ramos (111/116), iguais à medição local. Consulte o status do PR para o resultado da revisão mais recente.

## Próximos incrementos

1. Revisar e integrar Testcontainers após aprovação do responsável técnico.
2. Consolidar documentação, revisão final e defesa técnica; avaliar melhorias operacionais conforme o prazo.

A revisão final e a consolidação da entrega continuam pendentes.
