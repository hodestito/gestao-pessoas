# Gestão de pessoas — desafio técnico

Projeto em construção para o desafio técnico. Os primeiros fluxos implementados são a criação de departamentos e cargos; os fluxos de colaboradores e eventos ainda estão pendentes.

O modelo de domínio de Employee já contempla criação ativa, e-mail normalizado e inativação; ainda não há endpoint, persistência de colaboradores nem eventos.

## Documentação e evolução

- [Domínio](docs/domain.md): linguagem compartilhada, regras confirmadas, propostas e situação da modelagem DDD.
- [Casos de uso](docs/use-cases.md): os seis fluxos, contratos existentes/propostos e cenários de aceitação.
- [Decisão 001](docs/decisions/001-employee-e-kafka.md): identidade de Employee, regras aprovadas e direção para Kafka.
- [Uso de IA](AI_USAGE.md): participação da IA e padrão dos commits identificados com `[IA]`.

Os documentos distinguem requisitos do enunciado, definições do candidato, propostas e comportamento implementado. O Git registra a evolução real; os documentos apresentam o entendimento atual. A arquitetura detalhada e outros registros de decisão serão acrescentados conforme discutirmos essas decisões.

## Base escolhida

- Java 21 e Spring Boot 4.1.2
- Maven Wrapper, Spring Web, JPA, Flyway, validação, Actuator, H2 e driver PostgreSQL
- H2 é o banco usado por padrão; PostgreSQL pode ser selecionado pelo perfil `postgres`. O Flyway cria as tabelas de departamentos e cargos.

## Primeiro incremento

O domínio concentra validações e não depende do Spring ou do JPA. Os casos de uso chamam interfaces de repositório; adaptadores JPA fazem a persistência. A API valida as entradas e retorna `201 Created` com o identificador gerado.

Exemplos com a aplicação em execução na porta 8080:

```sh
curl -X POST http://localhost:8080/departments -H 'Content-Type: application/json' -d '{"name":"RH","costCenterCode":"CC-01"}'
curl -X POST http://localhost:8080/positions -H 'Content-Type: application/json' -d '{"title":"Engenheiro","level":"Sênior"}'
```

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

1. Modelar colaboradores com domínio independente de framework.
2. Implementar os casos de uso, persistência com migrações e endpoints restantes.
3. Implementar e testar publicação e consumo assíncronos com histórico.
4. Acrescentar testes de integração, Docker, documentação da API e registro do uso de IA.

A escolha da estratégia de mensageria e de garantias de entrega ainda precisa ser detalhada e validada ao longo da implementação.
