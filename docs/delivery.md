# Checklist de entrega e evidências

Revisão documental de 28/10/2026. Base funcional: main após PR #12, commit 997f9dc. O prazo interno acordado é 29/10/2026 pela manhã, horário de São Paulo; não é uma data exigida no arquivo do desafio.

## Requisitos do enunciado

| Requisito | Estado e evidência no repositório |
|---|---|
| Employee, Department e Position com os campos pedidos | Entidades em domain/employee, domain/department e domain/position; persistência separada em infrastructure/persistence |
| Seis endpoints obrigatórios | EmployeeController e OrganizationController; contratos e exemplos no README/OpenAPI |
| EmployeeCreatedEvent e EmployeeStatusChangedEvent | Records de domínio, Outbox, dois tópicos e consumidores Kafka |
| Consumidores com logs e tabela de histórico | EmployeeEventHistoryHandler/EmployeeHistoryWriter e employee_events, migration V4 |
| DDD e camadas domain/application/infrastructure/interfaces | Regras, valores e política no núcleo; [arquitetura e diagramas](architecture.md) |
| Domínio sem frameworks | Domínio e aplicação compilados com JDK 21 sem classpath externo |
| JPA e migrations | Adaptadores/entidades JPA; quatro versões em resources/db/migration |
| Testes unitários e integração com banco real | Suíte existente e perfil Maven testcontainers com PostgreSQL/Kafka descartáveis |
| Cobertura sugerida de 80% | Medição completa: 99,07% de linhas e 95,69% de ramos; não há gate percentual |
| Dockerfile, Compose e execução fora de container | Dockerfile em duas etapas, docker-compose.yml e comandos Maven no README |
| README com execução, API, eventos, banco padrão, Swagger e IA | README consolidado, links locais de Swagger e AI_USAGE.md |
| Rastreabilidade do uso de IA | Commits [IA], prompts efetivos, decisões e correções em AI_USAGE.md |
| Repositório público e envio do link | Pendente de alinhamento: repositório privado por orientação do candidato; nenhum envio à avaliadora realizado |

## Diferenciais e limites declarados

Outbox, Kafka, Testcontainers, GitHub Actions, Actuator e diagramas foram implementados. Logs customizados usam texto com campos/identificadores; não há configuração JSON de logging ou tracing distribuído. Os tópicos versionam seus contratos com .v1/schemaVersion, mas a API HTTP não tem versionamento por URI/header. Autenticação não consta dos endpoints exigidos e não foi implementada. Essas distinções evitam apresentar funcionalidades ausentes como entregues.

## Matriz de testes

Os caminhos abaixo são relativos a src/test/java/br/com/desafio/gestaopessoas/.

| Comportamento | Evidência representativa |
|---|---|
| Invariantes, valores e identidade | domain/employee/EmployeeTest, EmailTest, EmployeeRegistrationPolicyTest; domain/OrganizationEntityIdentityTest |
| Coordenação dos casos de uso | application/CreateEmployeeTest e DeactivateEmployeeTest |
| Cadastro HTTP, referências, unicidade e concorrência | EmployeeApiIntegrationTest |
| Listagem, ordenação, filtros, curingas e parâmetros SQL | EmployeeListApiIntegrationTest |
| Busca por departamento e homônimos | EmployeeDepartmentSearchIntegrationTest |
| Inativação, repetição, HTTP e controle otimista | EmployeeDeactivationApiIntegrationTest e infrastructure/persistence/EmployeeOptimisticLockIntegrationTest |
| Atomicidade Employee/Outbox e rollback | EmployeeOutboxIntegrationTest e EmployeeStatusOutboxIntegrationTest |
| Retry, falha de marcação e bloqueio de publicação | OutboxPublishingIntegrationTest |
| Kafka real, histórico, offset, duplicata e ordem inversa | EmployeeEventsKafkaIntegrationTest |
| Contrato HTTP, erros e recursos da UI | OpenApiIntegrationTest |
| Containers e conexões dinâmicas | support/IntegrationContainersConfiguration e WithIntegrationContainers, importados nos testes Spring |

Não afirmar que todo teste de concorrência HTTP força a mesma leitura de versão: o teste coordenado de persistência é a evidência específica da disputa. Não afirmar que falha simulada na marcação é um reinício real de processo. Os testes controlados de Outbox chamam o publicador; a demonstração Compose exercita o agendamento real.

## Resultados reproduzíveis

- Local, suíte completa com Testcontainers: 87 testes, zero falhas/erros/ignorados.
- Local, modo H2: 85 testes executados e dois Kafka ignorados, sem falhas/erros.
- [CI do PR #12](https://github.com/hodestito/gestao-pessoas/actions/runs/40714746417), revisão 01eabaa: 87 testes sem falhas/erros/ignorados; JAR/imagem construídos. Artefatos Surefire e JaCoCo baixados e conferidos.
- JaCoCo da suíte completa: 426/430 linhas e 111/116 ramos. Não soma medições de execuções diferentes.
- Docker: seis endpoints, 409/422, inativação repetida, publicação/consumo, reinício da aplicação e recriação dos containers preservando volumes foram validados na [decisão 012](decisions/012-docker-compose.md).

Reproduzir: `./mvnw -Ptestcontainers clean verify`. O modo H2 usa `./mvnw clean verify`. Consultar a [CI atual](https://github.com/hodestito/gestao-pessoas/actions) para a revisão mais recente. Artefatos remotos expiram após sete dias; a fonte e o comando permitem reproduzir a evidência. Cobertura não substitui avaliação das asserções nem comprova segurança/carga/alta disponibilidade.

## Validação desta documentação

O roteiro foi ensaiado tecnicamente em projeto Compose separado, gestao-apresentacao, com portas 18080/15432/19094 e dados fictícios. Os dez blocos shell passaram na verificação de sintaxe; os comandos de preparação, cadastro, consulta, inativação, SQL e logs foram executados. Os placeholders de IDs foram substituídos pelos valores retornados, como indicado no roteiro.

Foram confirmados três serviços saudáveis, os seis endpoints, e-mail normalizado, erros 409/422, duas inativações com 204 e duas ocorrências no histórico. Os links locais e cercas de código Markdown foram verificados. O ensaio técnico não equivale a um ensaio humano de fala ou duração. O ambiente descartável foi removido depois da validação; os dados de desenvolvimento foram preservados.

## Antes de apresentar e enviar

- [ ] Revisar e integrar o PR de consolidação documental após aprovação do candidato.
- [ ] Executar o ensaio humano usando [presentation.md](presentation.md), ajustando o tempo de fala e os IDs aos dados reais.
- [ ] Conferir JDK 21/Docker, imagens, portas, health e acesso à Swagger UI no ambiente da apresentação.
- [ ] Conferir CI da revisão final e guardar os relatórios se forem necessários após a retenção.
- [ ] Resolver o acesso da avaliadora: o enunciado solicita repositório público, enquanto o candidato pediu privado. Alinhar a aceitação dessa condição ou aprovar a mudança antes de enviar.
- [ ] Enviar o link à equipe avaliadora pelo canal combinado, depois de garantir que ela consegue acessar o conteúdo.

Essa lista não altera visibilidade, permissões ou canais de comunicação. O Swagger documentado é local e requer aplicação em execução; não há deployment público. A entrega técnica pode ser demonstrada sem publicar um serviço na internet.
