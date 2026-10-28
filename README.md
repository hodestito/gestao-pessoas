# Gestão de Pessoas — desafio técnico Itaú

API Java/Spring Boot para cadastrar departamentos, cargos e colaboradores, consultar colaboradores e registrar sua inativação. Criação e inativação geram eventos assíncronos com Kafka, Outbox transacional e histórico idempotente.

Os seis endpoints, eventos, persistência, documentação OpenAPI, Docker, CI e Testcontainers estão integrados à `main`. O candidato define e revisa as decisões técnicas; a participação da IA está registrada em [AI_USAGE.md](AI_USAGE.md).

## Comece por aqui

| Objetivo | Documento |
|---|---|
| Entender camadas, dependências, transações e eventos | [Arquitetura](docs/architecture.md) |
| Conhecer o modelo e as regras | [Domínio](docs/domain.md) e [casos de uso](docs/use-cases.md) |
| Preparar demonstração e defesa técnica | [Roteiro de apresentação](docs/presentation.md) |
| Executar endpoints com captura automática de IDs | [Collection Postman e environment local](postman/README.md) |
| Conferir requisitos, evidências e pendências | [Checklist de entrega](docs/delivery.md) |
| Consultar alternativas e evolução | [Índice das decisões](docs/decisions/README.md) |

## Tecnologias e modos de execução

Java 21, Spring Boot 4.1.2, Maven Wrapper, Spring MVC, Bean Validation, JPA/Hibernate, Flyway, PostgreSQL 16, H2, Kafka 4.1.0, springdoc 3.2.0, JaCoCo 0.8.16 e Testcontainers 2.0.5.

| Modo | Banco | Kafka | Pré-requisitos |
|---|---|---|---|
| Aplicação sem perfis | H2 em memória | Desabilitado; eventos ficam na Outbox | JDK 21 |
| Aplicação com perfis `postgres,kafka` | PostgreSQL | Habilitado | JDK 21 e serviços acessíveis |
| Aplicação no Compose | PostgreSQL em volume | Habilitado | Docker e Compose v2+ |
| Testes padrão | H2 em memória | Dois testes de broker ignorados | JDK 21 |
| Testes com `-Ptestcontainers` | PostgreSQL descartável | Broker descartável | JDK 21 e Docker |

H2 é o padrão da aplicação, mas o Compose ativa PostgreSQL/Kafka explicitamente. Java 21 é uma escolha técnica do projeto, não uma versão imposta pelo enunciado. O Wrapper fixa a versão do Maven; não exige Maven global. Downloads iniciais de dependências e imagens precisam de internet.

## Executar tudo com Docker Compose

Na raiz do repositório, com Docker em execução:

```sh
docker compose up -d --build --wait
docker compose ps
curl --fail http://localhost:8080/actuator/health
```

Acesse [Swagger UI](http://localhost:8080/swagger-ui.html), [OpenAPI JSON](http://localhost:8080/v3/api-docs) ou [OpenAPI YAML](http://localhost:8080/v3/api-docs.yaml). Os recursos da UI são servidos localmente. Suas requisições alteram os dados do ambiente conectado.

O Dockerfile compila com JDK 21/Maven Wrapper e copia o JAR para uma etapa com JRE 21 e usuário sem privilégios (UID 10001). O build usa `-DskipTests`; a suíte é executada separadamente na CI. O Compose aguarda PostgreSQL/Kafka saudáveis e consulta Actuator para verificar a aplicação.

As portas ficam restritas a `127.0.0.1`: aplicação 8080, PostgreSQL 5432 e Kafka 9092. As credenciais locais são `gestao`/`gestao_local`, banco `gestao_pessoas`. Entre containers, os endereços são `postgres:5432` e `kafka:19092`; `localhost` apontaria para o próprio container. APP_PORT, POSTGRES_PORT e KAFKA_HOST_PORT permitem trocar as portas do computador. O [roteiro](docs/presentation.md) mostra um projeto Compose separado para demonstração.

```sh
docker compose logs -f app
docker compose stop
```

`stop` preserva containers e dados. `down` remove containers/rede e preserva os volumes. `down -v` também apaga os volumes e seus dados. Após mudar fontes, execute novamente o comando com `--build`.

## Executar com Java no computador

Com JDK 21 selecionado:

```sh
java -version
./mvnw spring-boot:run
```

Esse modo usa H2 e perde os dados ao encerrar. Para executar com PostgreSQL e Kafka:

```sh
docker compose stop app
docker compose up -d --wait postgres kafka
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres,kafka
```

Pare a aplicação containerizada antes de executar outra instância sobre a mesma base. Isso evita conflito de porta e publicadores simultâneos. Para PostgreSQL sem Kafka, use somente o perfil `postgres`; os eventos permanecem pendentes na Outbox. No IntelliJ, importe o `pom.xml`, selecione JDK 21 e Maven Wrapper. No macOS, `export JAVA_HOME="$(/usr/libexec/java_home -v 21)"` seleciona um JDK 21 já instalado.

## Contrato HTTP

| Endpoint | Comportamento | Sucesso |
|---|---|---|
| POST /departments | Cadastra nome e centro de custo | 201 + Location |
| POST /positions | Cadastra título e nível | 201 + Location |
| POST /employees | Cadastra ACTIVE, com referências existentes | 201 + Location |
| GET /employees | Lista com filtros opcionais name/status | 200, página própria |
| GET /employees/search?department=RH | Busca departamento por nome exato, sem diferenciar caixa | 200, página própria |
| DELETE /employees/{id} | Inativa preservando o cadastro; repetição não cria outro evento lógico | 204; inexistente 404 vazio |

Exemplos na porta 8080:

```sh
curl -i -X POST http://localhost:8080/departments -H 'Content-Type: application/json' -d '{"name":"RH","costCenterCode":"CC-01"}'
curl -i -X POST http://localhost:8080/positions -H 'Content-Type: application/json' -d '{"title":"Engenheiro","level":"Sênior"}'
curl -i -X POST http://localhost:8080/employees -H 'Content-Type: application/json' -d '{"name":"Ana Silva","email":"ana@example.com","departmentId":"UUID-RETORNADO-DO-DEPARTAMENTO","positionId":"UUID-RETORNADO-DO-CARGO"}'
curl 'http://localhost:8080/employees?name=ana&status=ACTIVE&page=0&size=20'
curl 'http://localhost:8080/employees/search?department=RH&page=0&size=20'
curl -i -X DELETE 'http://localhost:8080/employees/UUID-RETORNADO-DO-COLABORADOR'
```

Substitua os três placeholders pelos UUIDs retornados. E-mail é normalizado e único inclusive para inativos. Entrada inválida retorna 400; e-mail duplicado, 409; departamento/cargo inexistente, 422; segundo conflito otimista na inativação, 409. Há dois formatos atuais de erro: `code/message` nas falhas tratadas e o formato padrão Spring nas demais; OpenAPI documenta essa diferença.

Para executar sem copiar IDs manualmente, importe a [collection Postman](postman/gestao-pessoas.postman_collection.json) e o [environment local](postman/local.postman_environment.json). O [guia](postman/README.md) orienta a execução sequencial dos seis endpoints e cenários negativos, com nomes/e-mail novos a cada rodada.

A página contém `content`, `page`, `size`, `totalElements` e `totalPages`. `page` começa em zero; `size` aceita 1–100, padrão 20. Ordenação crescente por `createdAt`, com `id` como desempate. A listagem inclui ativos/inativos quando `status` está ausente; `name` é parcial e trata %, _ e ! literalmente. A busca por departamento inclui todos os departamentos homônimos. Não há garantia de equivalência de acentos ou de estabilidade das páginas entre alterações dos dados.

## Arquitetura e consistência

O núcleo usa apenas Java: entidades com igualdade por ID, objetos de valor Email/CostCenterCode e EmployeeRegistrationPolicy como serviço de domínio. A aplicação coordena casos de uso e usa portas próprias. HTTP, Spring Data, JPA e Kafka ficam nas camadas externas. Os tipos EmployeePagination/PageResult evitam propagar Spring Data ao núcleo.

O fluxo e as dependências estão nos [diagramas de arquitetura](docs/architecture.md). Cadastro e inativação gravam Employee e seu evento na mesma transação. Kafka e histórico são etapas posteriores: `201`/`204` não significam que o consumidor já concluiu o processamento.

| Evento | Tópico |
|---|---|
| EmployeeCreatedEvent | employee-created.v1 |
| EmployeeStatusChangedEvent | employee-status-changed.v1 |

O publicador seleciona até 20 pendentes por ciclo (intervalo padrão de um segundo), aguarda confirmação do broker e só então marca `published_at`. Falhas de envio reagendam após cinco segundos. O consumidor confirma o offset após gravar o histórico; a chave única `event_id` trata reenvios. O grupo é `employee-history.v1`; cada tópico local possui uma partição e uma réplica.

Isso oferece entrega pelo menos uma vez com histórico idempotente, sob disponibilidade e retenção dos dados. Não há atomicidade entre PostgreSQL e Kafka, exactly-once ou ordem garantida entre tópicos/retries. O histórico registra fatos e não reconstrói o estado de Employee. Os payloads/logs customizados usam identificadores e metadados, sem nome/e-mail; os IDs ainda são correlacionáveis. Limites e alternativas estão na [arquitetura](docs/architecture.md).

## Testes e cobertura

Verificação rápida, sem exigir Docker:

```sh
./mvnw clean verify
```

Suíte completa com PostgreSQL/Kafka descartáveis, sem preparar Compose ou banco:

```sh
./mvnw -Ptestcontainers clean verify
```

O perfil Maven ativa os containers e os dois testes Kafka. `@ServiceConnection` fornece as portas dinâmicas ao Spring/Flyway. O isolamento é por contexto Spring; contextos compartilhados dependem da limpeza das fixtures. Docker inacessível reprova o modo completo, sem fallback silencioso. Os módulos Testcontainers têm escopo de teste e não entram no JAR de produção.

Medição de 28/10/2026, confirmada localmente e na [CI com Testcontainers](https://github.com/hodestito/gestao-pessoas/actions/runs/40714746417): 87 testes, zero falhas/erros/ignorados. O modo padrão executou 85 e ignorou os dois Kafka, sem falhas.

| Escopo | Linhas | Ramos |
|---|---:|---:|
| Total | 99,07% (426/430) | 95,69% (111/116) |
| Domínio | 100% (92/92) | 90,48% (38/42) |
| Aplicação | 100% (60/60) | 100% (30/30) |
| Infraestrutura | 99,12% (225/227) | 96,55% (28/29) |
| Interfaces | 100% (48/48) | 100% (15/15) |

JaCoCo gera HTML em `target/site/jacoco/index.html` e XML em `target/site/jacoco/jacoco.xml` na fase `verify`. `test` coleta dados, mas não atualiza esses relatórios. Cada execução substitui a medição anterior; `clean` remove os artefatos. Não há exclusões manuais nem gate percentual. Cobertura indica execução, não ausência de defeitos; testes, cenários e limites constam no [checklist de entrega](docs/delivery.md).

## CI e versionamento

O [workflow CI](.github/workflows/ci.yml) roda em PRs para `main` e pushes nela: Temurin 21/cache Maven, suíte Testcontainers, construção da imagem e publicação de relatórios Surefire/JaCoCo por sete dias. No artefato `verificacao-java-postgres-kafka`, abra `site/jacoco/index.html`. A saída dos testes está nos relatórios Surefire; logs completos do broker não são um artefato separado.

A CI constrói a imagem, mas o roteiro completo dentro dos containers é validado separadamente. O resultado sinaliza falhas; proteção de branch obrigatória não foi configurada. As actions são fixadas por SHA e o token usa leitura do conteúdo.

O fluxo Git usa `main` estável, branches curtas e merge após revisão/aprovação do candidato. Commits assistidos por IA levam `[IA]`; [AI_USAGE.md](AI_USAGE.md) registra prompts reais, decisões e correções.

## Entrega e limites

A solução atende aos fluxos técnicos documentados. Autenticação, versionamento da API HTTP, DLQ, limpeza/retenção automática de Outbox/histórico, teste de carga e alta disponibilidade não foram implementados. O publicador foi validado com uma instância e mantém conexão/bloqueio durante o envio. Consulte as consequências na [arquitetura](docs/architecture.md).

O repositório permanece privado por decisão do candidato, enquanto o enunciado solicita público. Antes do envio, é necessário alinhar o acesso com a avaliadora ou revisar essa decisão. Essa pendência, a revisão final e o ensaio estão no [checklist de entrega](docs/delivery.md). Não há publicação externa do Swagger: o link acima funciona com a aplicação local em execução.
