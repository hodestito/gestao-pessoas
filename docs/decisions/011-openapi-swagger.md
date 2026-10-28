# 011 — Contrato HTTP com OpenAPI e Swagger UI

Estado: integrado à main pelo PR #9 após validação do responsável técnico. Data: 28/10/2026. O PR #8 foi integrado à main após autorização explícita, antes desta etapa.

## Objetivo e escolha

Disponibilizar um contrato navegável dos seis endpoints existentes, com exemplos, entradas obrigatórias, paginação, respostas e limites da confirmação assíncrona. O incremento não altera regras de negócio, persistência ou contratos Kafka.

Foi escolhido springdoc-openapi-starter-webmvc-ui 3.2.0, compatível com Spring Boot 4.1.x segundo a matriz oficial. As anotações ficam nos controllers e DTOs da interface REST; OpenApiConfig, na infraestrutura, fornece metadados e esquemas compartilhados de erro. Domínio e aplicação continuam sem imports de frameworks de documentação.

Alternativas consideradas:

- Um arquivo YAML escrito manualmente permitiria trabalhar a partir do contrato, mas duplicaria os mappings e exigiria sincronização explícita com o código existente.
- Somente a descoberta automática reduziria anotações, mas não explicaria idempotência, garantia de commit, concorrência e formatos reais de erro.
- Anotações próximas aos métodos HTTP mantêm a descrição junto à entrada correspondente, ao custo de aumentar o tamanho dos controllers. Não foram extraídas interfaces adicionais apenas para mover essas anotações.

OpenAPI é a especificação gerada; Swagger UI é a interface que a apresenta. As URLs são /v3/api-docs, /v3/api-docs.yaml e /swagger-ui.html. Os recursos da interface são servidos localmente. Não há um documento exportado versionado que possa divergir do gerado em execução.

## Fidelidade ao comportamento atual

O documento restringe a descoberta ao pacote REST. Não inclui Actuator como operação de negócio. A inclusão automática de todas as respostas do ControllerAdvice em todos os endpoints foi desativada; cada operação descreve suas respostas aplicáveis.

Foram preservados dois formatos observados por requisições HTTP antes da alteração: ApiError, com code/message, e FrameworkError, com timestamp/status/error/path. BadRequest usa oneOf para indicar as alternativas. A futura padronização dos erros exige um incremento próprio. O 404 de inativação não tem corpo; 204 também não possui conteúdo.

As descrições explicitam:

- Cadastro ativo, e-mail normalizado e único inclusive para inativos; referências inexistentes retornam 422.
- UUIDs ilustrativos precisam ser substituídos pelos IDs efetivamente cadastrados.
- Nome parcial, departamento exato sem diferenciar caixa e inclusão de departamentos homônimos.
- Página iniciada em zero, padrão de 20 itens, tamanho de 1 a 100, ordenação createdAt/id crescente e limites de deslocamento.
- Inativação repetida retorna 204 sem novo evento lógico; um segundo conflito otimista retorna 409.
- 201/204 confirmam a operação no banco e a intenção na Outbox; publicação e histórico Kafka continuam assíncronos.

Os valores padrão de page/size foram declarados também no Schema, pois a combinação inicial de Parameter com RequestParam não os expôs corretamente no documento gerado. O teste verificou o JSON real; a correção não alterou os padrões da API.

## Verificação e limites

OpenApiIntegrationTest consulta um servidor HTTP real em porta aleatória. Os quatro testes verificam as seis operações, códigos de resposta, Location, ausência de corpo documentado em 204/404, resolução de todas as referências locais, entradas obrigatórias, limites e padrões de paginação, enum de status e formatos UUID/instante. Também conferem exemplos reais dos dois formatos de erro, 404 vazio, configuração e recursos locais da UI e exportação YAML.

As anotações não executam validação de negócio. Esses testes não constituem validação formal completa da especificação nem teste visual da interface; os testes funcionais existentes continuam exercitando HTTP, persistência, Outbox e Kafka. O contrato OpenAPI descreve HTTP; os contratos dos eventos permanecem nas decisões 006 e 007.

Resultado final: clean verify com Kafka real e H2 passou com 87 testes, sem falhas, erros ou ignorados. A execução separada com PostgreSQL 16.15 na base exclusiva gestao_outbox_tests_20261027_01 e Kafka real também passou com os mesmos 87 testes. Ambas geraram o JAR e os mesmos contadores JaCoCo: 426/430 linhas (99,07%) e 111/116 ramos (95,69%). A nova configuração acrescentou 18 linhas mensuráveis; as regras existentes não mudaram. Os 28 arquivos de domínio/aplicação também compilaram diretamente com javac --release 21, sem classpath externo.

## Arquivos

- pom.xml e application.properties: dependência, descoberta restrita e configuração da UI.
- OpenApiConfig: metadados e composição dos esquemas de erro.
- EmployeeController e OrganizationController: operações, parâmetros, respostas e DTOs HTTP.
- EmployeeExceptionHandler: nome/esquema da resposta existente; tratamento inalterado.
- OpenApiIntegrationTest: verificação do documento e dos recursos servidos.
- README e AI_USAGE: acesso à documentação, resultados e participação da IA; decisão 010 registra a integração do PR #8.

## Referências

- [Compatibilidade springdoc / Spring Boot](https://springdoc.org/faq.html).
- [Configuração inicial](https://springdoc.org/getting-started.html).
- [Propriedades springdoc](https://springdoc.org/properties.html).
