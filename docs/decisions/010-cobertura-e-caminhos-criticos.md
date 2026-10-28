# 010 — Cobertura e caminhos críticos

Estado: integrado à main pelo PR #8, após aprovação explícita do responsável técnico. O PR #7 foi integrado antes do início desta etapa. Data: 28/10/2026. As medições abaixo registram o estado desse incremento; novas classes podem alterar os denominadores.

## Objetivo e abordagem

O enunciado sugere cobertura mínima de 80%, sem definir o contador. Este incremento mede linhas e ramos separadamente e usa o relatório para localizar testes de comportamento relevantes. A primeira medição já superava os 80% de linhas; não foi necessário modificar classes funcionais para atingir a referência.

JaCoCo 0.8.16 foi fixado no Maven. prepare-agent disponibiliza o agente na JVM de testes, e report gera HTML/XML/CSV no verify. O agente faz instrumentação durante a execução dos testes; a configuração é um plugin de build, não uma dependência do domínio ou da aplicação.

append=false substitui os dados da execução anterior. Os comandos documentados usam clean verify para remover artefatos e relatórios antigos. Executar apenas test coleta dados, mas não atualiza o relatório ligado à fase verify.

Não há exclusões manuais de classes nem gate percentual jacoco:check. O build exige a aprovação dos testes; os 80% são uma referência documentada. Um gate de qualidade poderá ser definido na etapa de CI com aprovação do responsável técnico.

## Medição inicial e escolha dos testes

Medição inicial com H2 e Kafka real, 74 testes sem falhas/erros/ignorados:

| Escopo | Linhas | Ramos |
|---|---:|---:|
| Total | 94,90% (391/412) | 81,03% (94/116) |
| Domínio | 100% (92/92) | 90,48% (38/42) |
| Aplicação | 100% (60/60) | 100% (30/30) |
| Infraestrutura | 92,82% (194/209) | 62,07% (18/29) |
| Interfaces | 91,67% (44/48) | 53,33% (8/15) |

A análise priorizou lacunas em falhas e contratos, mantendo os testes de domínio existentes. Não foram acrescentados testes de acessores ou casos artificiais apenas para buscar 100%.

| Arquivo | Comportamentos acrescentados |
|---|---|
| OutboxPollerTest | Um evento adiado, cujo publicador retorna false, não impede tentar o próximo selecionado; interrupção interrompe o lote antes de iniciar outro evento. Também verifica limite de seleção de 20. |
| EmployeeMessageContractTest | Os dois consumidores rejeitam versão não suportada e tipo incorreto antes de chamar o histórico. A entrada é JSON e o teste verifica a causa de validação. |
| KafkaOutboxDeliveryTest | Um tipo de evento desconhecido falha sem publicar no tópico de outro contrato. |
| EmployeeExceptionHandlerTest | Constraints conhecidas, inclusive mensagens aninhadas/maiúsculas, mantêm seus códigos; integridade desconhecida é propagada; conflito otimista retorna 409 explícito. |
| EmployeeListApiIntegrationTest | A API lista ativos e inativos sem filtros e trata nome em branco como ausência de filtro, exercitando a Specification real com nome/status ausentes. |

São nove testes adicionais, totalizando 83 com Kafka habilitado. A continuidade do lote foi testada para envio adiado tratado pelo publicador; isso não afirma que uma exceção não tratada de banco permita continuar o mesmo ciclo.

## Resultado final

Execuções separadas, sem combinar dados:

- H2 + Kafka real: clean verify -Dkafka.tests=true, 83 testes sem falhas, erros ou ignorados; jar gerado.
- PostgreSQL 16.15 + Kafka real: clean verify com o perfil postgres e a base exclusiva gestao_outbox_tests_20261027_01, 83 testes sem falhas, erros ou ignorados; jar gerado.

Ambas produziram os mesmos contadores:

| Escopo | Linhas | Ramos |
|---|---:|---:|
| Total | 99,03% (408/412) | 95,69% (111/116) |
| Domínio | 100% (92/92) | 90,48% (38/42) |
| Aplicação | 100% (60/60) | 100% (30/30) |
| Infraestrutura | 99,04% (207/209) | 96,55% (28/29) |
| Interfaces | 100% (48/48) | 100% (15/15) |

Os contadores foram extraídos de target/site/jacoco/jacoco.xml. O relatório analisou 58 classes de produção com código mensurável. Os números incluem os filtros automáticos da ferramenta para código gerado; não foram configuradas exclusões próprias.

O total considera também a classe de entrada da aplicação, fora dos quatro grupos de camada. Por isso, somar apenas as linhas das quatro camadas não reproduz todo o denominador.

## Como reproduzir e consultar

Os comandos completos para H2 e PostgreSQL estão no README. Os serviços locais PostgreSQL/Kafka precisam estar disponíveis para a execução que os utiliza. Sem -Dkafka.tests=true, os dois testes Kafka reais ficam ignorados e o percentual pode mudar.

Artefatos locais, não versionados:

- target/site/jacoco/index.html: relatório navegável.
- target/site/jacoco/jacoco.xml: contadores e dados por classe/método/linha.
- target/site/jacoco/jacoco.csv: formato tabular.
- target/jacoco.exec: dados binários da última execução.

No Mac, open target/site/jacoco/index.html abre o relatório. A execução seguinte substitui os dados; clean remove esses arquivos. Os relatórios não foram incorporados ao Git para evitar versionar artefatos de build.

## Interpretação e limites

Cobertura de linha indica que ao menos uma instrução associada à linha foi executada. Ramos medem alternativas de if/switch; try/catch não entra diretamente nesse contador. Uma linha pode estar coberta mesmo sem todas as alternativas exercitadas. Os testes de falhas continuam necessários, independentemente do percentual.

A medição combina os testes unitários e de integração executados nessa JVM. Ela não separa uma “cobertura unitária” do que foi executado por testes HTTP/banco/Kafka. Os testes novos dos contratos e do poller são unitários; não simulam por si só reinício de processo, perda de rede ou agendamento real pelo Spring.

Permanecem lacunas visíveis no HTML, incluindo o ponto de entrada main, trechos de instanciação/reconstituição JPA, caminhos de igualdade e interrupção dentro da transação de publicação. Não foram criados testes apenas para executar essas linhas. Cobertura alta não comprova todas as entradas possíveis, qualidade das asserções, segurança, desempenho ou ausência de corridas.

## Escopo de arquivos

pom.xml foi alterado para configurar a ferramenta. Foram criadas quatro classes de teste, ampliado EmployeeListApiIntegrationTest e atualizados README, AI_USAGE, docs/domain.md e o estado da decisão 009. Nenhum arquivo de src/main/java, migração, contrato HTTP ou contrato Kafka foi alterado.

## Referências

- [JaCoCo: releases](https://www.jacoco.org/jacoco/index.html).
- [Plugin Maven](https://www.jacoco.org/jacoco/trunk/doc/maven.html).
- [prepare-agent](https://www.jacoco.org/jacoco/trunk/doc/prepare-agent-mojo.html).
- [report](https://www.jacoco.org/jacoco/trunk/doc/report-mojo.html).
- [Definição dos contadores](https://www.jacoco.org/jacoco/trunk/doc/counters.html).
