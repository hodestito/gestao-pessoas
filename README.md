# Gestão de pessoas — desafio técnico

Projeto em construção para o desafio técnico. Estão implementados a criação de departamentos, cargos e colaboradores, a listagem e a busca de colaboradores por departamento. Inativação via API e eventos ainda estão pendentes.

Employee contempla criação ativa, e-mail normalizado e inativação no domínio. A API de cadastro persiste o colaborador; ainda não publica eventos.

## Documentação e evolução

- [Domínio](docs/domain.md): linguagem compartilhada, regras confirmadas, propostas e situação da modelagem DDD.
- [Casos de uso](docs/use-cases.md): os seis fluxos, contratos existentes/propostos e cenários de aceitação.
- [Decisão 001](docs/decisions/001-employee-e-kafka.md): identidade de Employee, regras aprovadas e direção para Kafka.
- [Decisão 002](docs/decisions/002-cadastro-employee.md): transação, persistência e respostas do cadastro.
- [Decisão 003](docs/decisions/003-listagem-colaboradores.md): Page, filtros e limites da listagem.
- [Decisão 004](docs/decisions/004-busca-por-departamento.md): busca por departamento e critérios adotados para revisão.
- [Uso de IA](AI_USAGE.md): participação da IA e padrão dos commits identificados com `[IA]`.

Os documentos distinguem requisitos do enunciado, definições do candidato, propostas e comportamento implementado. O Git registra a evolução real; os documentos apresentam o entendimento atual. A arquitetura detalhada e outros registros de decisão serão acrescentados conforme discutirmos essas decisões.

## Base escolhida

- Java 21 e Spring Boot 4.1.2
- Maven Wrapper, Spring Web, JPA, Flyway, validação, Actuator, H2 e driver PostgreSQL
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
```

Substitua os identificadores pelos UUIDs retornados pelos dois primeiros cadastros. E-mail duplicado retorna 409; departamento ou cargo inexistente retorna 422. O cadastro de colaboradores ainda não emite o evento obrigatório, que será implementado no incremento de eventos com Kafka.

A listagem aceita filtros opcionais `name` e `status`, páginas iniciadas em zero e tamanho de 1 a 100 (padrão 20). Busca por nome é parcial e insensível a caixa, sem garantia de ignorar acentos. A resposta contém `content`, `page`, `size`, `totalElements` e `totalPages`. Consulte [casos de uso](docs/use-cases.md) para os limites.

A busca por departamento usa correspondência exata sem diferenciar maiúsculas/minúsculas, inclui ativos e inativos e retorna colaboradores de todos os departamentos com o mesmo nome. Department ausente ou vazio resulta em 400; nenhum resultado retorna 200 com conteúdo vazio.

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

Com o Docker Desktop em execução, use `docker compose up -d` nesta pasta. A conexão é limitada ao próprio computador, na porta 5432, com banco `gestao_pessoas`, usuário `gestao` e senha local `gestao_local`. Os dados ficam em um volume Docker. Essas credenciais são apenas para desenvolvimento.

Para iniciar a aplicação usando esse banco: `./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres`. Sem esse perfil, a base inicial usa H2 em memória. Para parar o banco sem apagar os dados, use `docker compose stop`.

## Próximos incrementos

1. Implementar inativação de colaboradores pela API.
2. Revisar regras de domínio e casos de uso com o responsável técnico.
3. Implementar e testar publicação e consumo assíncronos com histórico.
4. Acrescentar testes de integração, Docker, documentação da API e registro do uso de IA.

A escolha da estratégia de mensageria e de garantias de entrega ainda precisa ser detalhada e validada ao longo da implementação.
