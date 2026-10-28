# 014 — Testcontainers para PostgreSQL e Kafka

Estado: integrado à main pelo PR #12 após aprovação do responsável técnico, com validação local e remota. Data: 28/10/2026. A adoção, antes adiada, foi solicitada pelo responsável técnico após a integração do PR #11.

## Objetivo e alternativas

Eliminar a preparação manual de banco/broker para os testes de integração e a dependência de portas fixas. O Compose permanece como ambiente de execução da aplicação; o ambiente de testes passa a ter seu ciclo de vida controlado pelo próprio teste.

Foi escolhido um perfil Maven testcontainers que habilita integration.containers e kafka.tests na JVM Surefire. O comando ./mvnw -Ptestcontainers clean verify executa a suíte com PostgreSQL e Kafka descartáveis. O modo padrão ./mvnw test mantém H2 e não exige Docker; os dois testes Kafka continuam explicitamente ignorados nesse modo.

Tornar containers obrigatórios em toda execução simplificaria o comando padrão, mas obrigaria Docker mesmo para a verificação rápida com H2. Criar novas cópias dos testes para PostgreSQL duplicaria cenários e manutenção. O perfil escolhido executa os mesmos testes, alterando somente a infraestrutura fornecida. Não foi introduzida uma segunda suíte em Failsafe nem alterada a convenção dos nomes existentes.

## Configuração e conexão

O Spring Boot 4.1.2 gerencia Testcontainers 2.0.5. Foram acrescentados spring-boot-testcontainers, testcontainers-postgresql e testcontainers-kafka, todos com escopo test. Esses módulos não entram no JAR de produção.

IntegrationContainersConfiguration é uma TestConfiguration condicional a integration.containers=true. Declara PostgreSQLContainer postgres:16 e, apenas no contexto com perfil Spring kafka, KafkaContainer apache/kafka:4.1.0. Usa as mesmas linhas de imagem do Compose, mas o módulo Kafka configura seus próprios listeners e portas. Não há binding fixo para portas no computador.

@ServiceConnection cria os detalhes de conexão JDBC/Flyway/Kafka e os fornece à autoconfiguração. Isso tem precedência sobre propriedades de URL/conexão externas, evitando conectar a uma base do desenvolvedor quando o modo completo está ativo. Flyway executa as quatro migrações em cada base nova.

WithIntegrationContainers é uma anotação de teste que importa essa configuração. As doze classes SpringBootTest existentes a usam. A configuração fica inativa no modo padrão; testes unitários não importam nem iniciam containers.

EmployeeEventsKafkaIntegrationTest deixou de ler bootstrap.servers diretamente por @Value e passa os dados efetivos de KafkaAdmin ao cliente de administração utilizado na verificação de offsets. Uma propriedade textual pode continuar com seu valor padrão mesmo quando ServiceConnection fornece outro endereço à autoconfiguração; usar a configuração efetiva evita consultar o broker local por engano.

## Ciclo de vida e isolamento

O Spring Boot gerencia o início e a parada dos containers declarados como beans. Cada contexto Spring possui seu PostgreSQL; contextos reutilizados pelo cache de testes compartilham o mesmo banco/container. Isso reduz inicializações, mas não oferece isolamento por método. As fixtures existentes continuam limpando os registros criados pelos testes. Não foi habilitada execução paralela dos métodos.

Kafka é criado somente para os testes reais com perfil kafka. OutboxPublishingIntegrationTest mantém delivery simulado e listeners/admin desabilitados, portanto não precisa de broker. Os tópicos/grupo do teste Kafka continuam exclusivos e removidos no teardown.

Não foram habilitados containers reutilizáveis entre execuções nem desativado Ryuk. Em uma execução normal, a parada dos contextos e o mecanismo de limpeza Testcontainers removem os recursos. Docker inacessível reprova o modo completo; não há disabledWithoutDocker ou fallback silencioso para H2.

O custo é inicialização, memória e download inicial de imagens. No Mac, o Docker Desktop foi descoberto pelo socket do usuário sem configuração global adicional. No runner Linux, o Docker já disponível é usado diretamente.

## CI e reprodução

O workflow passa a executar ./mvnw -B -ntp -Ptestcontainers clean verify. Foram removidas criação manual de banco, inicialização/limpeza de serviços Compose e coleta de compose.log. Os relatórios Surefire incluem a saída capturada da aplicação/testes; JaCoCo continua publicado por sete dias. Os logs completos do broker não são exportados como artefato separado neste incremento.

A construção da imagem continua depois do sucesso dos testes, usando o Compose. Dockerfile não foi modificado e segue com -DskipTests. A migração afeta exclusivamente infraestrutura de teste, Maven, CI e documentação; nenhuma classe de src/main/java, migração ou contrato foi alterado.

## Verificação local

./mvnw -Ptestcontainers clean verify passou com 87 testes, zero falhas, erros ou ignorados. Os logs confirmaram sete PostgreSQL descartáveis para os contextos e um Kafka, além de Ryuk. As URLs JDBC usaram portas dinâmicas, por exemplo 60403 e 60440, e as quatro migrações foram aplicadas. Após a JVM de testes encerrar, nenhum container com label org.testcontainers=true permaneceu ativo.

JaCoCo manteve 426/430 linhas (99,07%) e 111/116 ramos (95,69%). A configuração nova está em src/test e não altera o denominador de produção. A [execução remota 40714746417](https://github.com/hodestito/gestao-pessoas/actions/runs/40714746417), revisão 01eabaa, também passou com 87 testes sem falhas/erros/ignorados e construção Docker concluída. Os artefatos baixados confirmaram os mesmos contadores.

O modo padrão clean verify também passou: 87 testes contabilizados, 85 executados, dois Kafka ignorados e zero falhas/erros. Não houve descoberta de Docker ou criação de containers nesse log. A inspeção do JAR confirmou ausência de bibliotecas Testcontainers em BOOT-INF/lib.

## Referências

- [Spring Boot: Testcontainers e Service Connections](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html).
- [Módulo PostgreSQL](https://java.testcontainers.org/modules/databases/postgres/).
- [Módulo Kafka](https://java.testcontainers.org/modules/kafka/).
