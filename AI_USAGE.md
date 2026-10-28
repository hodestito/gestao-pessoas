# Uso de IA Generativa no Projeto

## Ferramentas utilizadas

- **Codex/ChatGPT:** apoio à modelagem de domínio, discussão de alternativas, implementação, testes e documentação. O responsável técnico revisa as propostas e toma as decisões arquiteturais.
- **Spring Initializr:** gerou a estrutura inicial do projeto; não é uma ferramenta de IA.

## Prompts e interações efetivas

### Cobertura e testes de falha

**Instrução:** “Faça o merge e siga para o proximo passo.”

**Resultado:** após integrar o PR #7, a IA seguiu a etapa de medir cobertura do plano aprovado. Configurou JaCoCo e analisou o relatório antes de escolher testes adicionais. A medição inicial já superava os 80% sugeridos; foram priorizadas falhas de contrato, roteamento, processamento do lote e tradução de integridade, sem modificar código funcional nem excluir classes do relatório.

### Independência do núcleo e identidade das entidades

**Instrução:** “Quero deixar o dominio independente de Spring Data e deixar coerente as entidades Department e Position, identificadas por ID.”

**Resultado:** o domínio já era independente de Spring Data; a revisão retirou essa dependência também da aplicação. Foram criados tipos próprios de paginação e a conversão ficou no adaptador JPA. Department/Position passaram de records para classes finais com igualdade/hashCode por ID. Foram discutidas as alternativas de manter Page na aplicação e de sobrescrever igualdade nos records. A independência foi verificada compilando domínio/aplicação apenas com o JDK 21.

### Serviço de domínio

**Pergunta e instrução:** “O que é serviço de dominio?” e “Crie as regras numa classe de serviço de dominio”.

**Resultado:** foram comparadas a coordenação na aplicação e a política de admissão no domínio. O responsável técnico aprovou a extração das regras existentes, sem inventar novas condições. EmployeeRegistrationPolicy consulta uma porta do domínio implementada por JPA; a exceção de regra foi movida para o domínio. Os testes separam decisão de negócio, coordenação e integração HTTP.

### Evento de inativação

**Instrução e decisão:** “ok. Vamos para o proximo passo” e “Dois tópicos, preservando o atual”.

**Resultado:** após apresentar classes e alternativas, foi implementado o fluxo de inativação com Outbox JPA, contrato específico e consumidor Kafka. O compartilhamento do histórico ficou em um handler e em metadados comuns; a desserialização continua usando um tipo concreto por tópico. Os testes incluem rollback da versão/status, repetição, concorrência e reenvio.

### Modelagem de domínio

**Pergunta:** “Temos um arquitetura definida? Usamos DDD antes de começar a criar classes?”

**Resultado:** a revisão identificou que a separação técnica em camadas já existia, mas a modelagem de domínio estava incompleta. Foram documentados linguagem compartilhada, regras confirmadas, hipóteses e casos de uso em `docs/domain.md` e `docs/use-cases.md`.

### Escolha de mensageria

**Pergunta:** “Por que Sprint Events e não Kafka que é o recomendado?”

**Resultado:** foram comparados Spring Events e Kafka. O responsável técnico escolheu Kafka, em linha com a preferência do enunciado. Posteriormente aprovou Outbox para eliminar a janela entre commit e registro da intenção de publicação.

### Consistência e entrega de eventos

**Perguntas e instrução:** “O quanto acrescentaria de complexidade se consideramos outbox?” e “Implemente com outbox”.

**Resultado:** a proposta de listener AFTER_COMMIT foi substituída por Outbox transacional com polling. A IA implementou o fluxo de cadastro, publicação Kafka e histórico idempotente. Foram explicitados reenvios, falhas de confirmação, bloqueio de linha durante o envio e o limite de uma instância validada. O evento de inativação foi acrescentado no incremento descrito na decisão 007.

### Consulta e segurança

**Pergunta:** “Estamos seguros contra injection?”

**Resultado:** foram distinguidos a parametrização JDBC e o escape de curingas do `LIKE`. A revisão resultou em testes de caracteres especiais, tentativa de SQL injection e inspeção do SQL gerado, executados em H2 e PostgreSQL.

### Documentação HTTP

**Instrução:** “Seguimos. Proximo passo”. O plano apresentado foi documentar os seis endpoints com OpenAPI/Swagger, após a etapa de cobertura. O merge do PR #8 recebeu aprovação explícita separada.

**Resultado:** a IA acrescentou springdoc-openapi 3.2.0, configuração de metadados/esquemas e anotações nos controllers e DTOs HTTP. Foram comparadas descoberta automática, anotações e YAML manual; a documentação próxima dos endpoints evita duplicar o contrato em um arquivo estático. Domínio e aplicação não receberam dependências de documentação.

**Revisão e validação:** respostas HTTP reais foram inspecionadas antes da documentação para preservar os dois formatos de erro existentes. Os testes verificam o documento gerado, referências, paginação, códigos de resposta e recursos locais da UI. Um teste detectou a ausência dos padrões de paginação no Schema; a descrição foi corrigida sem alterar a API. A decisão 011 registra alternativas, limites e resultados.

### Execução com Docker e Compose

**Instrução:** “Validado. Vamos seguir com dockerfile e compose”. A documentação OpenAPI foi integrada pelo PR #9 após a validação do responsável técnico.

**Resultado:** após apresentar o plano, a IA criou Dockerfile com etapas JDK/JRE 21, Maven Wrapper e usuário sem privilégios, restringiu o contexto com .dockerignore e acrescentou app ao Compose existente. As conexões usam nomes dos serviços, com healthchecks de inicialização e portas externas configuráveis. Não foram alteradas classes Java.

**Decisões e limites:** foram comparados build dentro do Docker e cópia de JAR previamente compilado. O empacotamento da imagem ignora execução de testes; a validação usa a suíte separada e o fluxo HTTP/Kafka no ambiente final. A checagem Kafka usa o listener interno, pois a porta publicada no Mac pode mudar. A validação usa projeto Compose, portas e volumes próprios, preservando os dados de desenvolvimento. A decisão 012 registra comandos, alternativas e resultados reais.

### Integração contínua

**Instrução:** “Aprovado.”, após a revisão de Docker/Compose e a indicação de CI como próximo incremento.

**Resultado:** a IA integrou o PR #10 e apresentou o plano de um workflow com JDK 21, Maven Wrapper, PostgreSQL/Kafka reais, relatórios e construção da imagem. O workflow reutiliza o Compose existente, cria uma base exclusiva no runner e habilita explicitamente os testes Kafka. As actions oficiais foram consultadas e fixadas por SHA.

**Decisões e limites:** optou-se por um job com PostgreSQL/Kafka em vez de duplicar configuração de serviços ou criar matriz H2/PostgreSQL. A imagem só é construída após os testes; sua execução completa foi validada na etapa Docker. Não foram introduzidos gate percentual ou proteção de branch. A decisão 013 distingue implementação do workflow de comprovação de execução remota.

## Decisões influenciadas por IA e revisão técnica

1. **Domínio e arquitetura:** a IA propôs uma organização em camadas e a separação do domínio de frameworks. O modelo foi revisto com o responsável técnico; os limites e as decisões permanecem documentados em `docs/decisions/`.
2. **Cadastro:** após discussão, o responsável técnico aprovou e-mail único inclusive para inativos, departamento e cargo existentes, HTTP 409 para e-mail duplicado, HTTP 422 para referências inexistentes e uma transação por cadastro. Uma constraint no banco complementa a consulta prévia de e-mail.
3. **Listagem e busca:** inicialmente o responsável técnico escolheu `Page` do Spring Data. Após revisão arquitetural, solicitou eliminar esse acoplamento do núcleo. A aplicação agora usa EmployeePagination e PageResult, com conversão na infraestrutura. O custo potencial de contagem e os limites por deslocamento permanecem. A busca por departamento mantém correspondência exata sem diferenciar caixa e inclui homônimos.
4. **Mensageria:** Kafka e Outbox foram escolhidos pelo responsável técnico após comparação de alternativas. Empregado e evento são gravados na mesma transação; o publicador aguarda confirmação do broker e o consumidor deduplica por eventId. Após revisão, o responsável técnico escolheu JPA também para Outbox/histórico para uniformizar a persistência. O bloqueio do publicador é pessimista; o controle otimista de Employee permanece separado.
5. **Inativação e concorrência:** o responsável técnico escolheu `@Version` após comparar bloqueio otimista, pessimista e atualização condicional. A IA implementou a releitura limitada após conflito e, neste incremento, a gravação de EmployeeStatusChangedEvent na mesma transação da mudança. Repetição sem transição não gera outro evento lógico.
6. **Tópicos e contratos:** ao comparar um tópico comum com dois tópicos, o responsável técnico escolheu “Dois tópicos, preservando o atual”. A IA acrescentou employee-status-changed.v1 e manteve employee-created.v1, reutilizando Outbox e histórico. A ausência de ordem entre tópicos foi documentada e testada processando a inativação antes da criação.
7. **Admissão no domínio:** o responsável técnico solicitou uma classe de serviço de domínio para as regras de departamento, cargo e e-mail. A IA implementou uma política independente de frameworks, uma porta de consulta e o adaptador JPA. CreateEmployee mantém a coordenação; o banco continua sendo a garantia final sob concorrência.
8. **Identidade das entidades:** o responsável técnico solicitou igualdade por ID para Department/Position. A IA manteve atributos, validações e acessores, usando classes finais em coerência com Employee; objetos de valor continuam com igualdade por valor. Testes verificam atributos diferentes com o mesmo ID e atributos iguais com IDs diferentes.
9. **Cobertura:** JaCoCo foi acrescentado ao build para medir linhas e ramos. Os testes novos foram escolhidos pela relevância dos fluxos de falha, não por uma meta de 100%. O resultado com H2/Kafka e PostgreSQL/Kafka foi documentado separadamente; o indicador não é apresentado como garantia de ausência de defeitos.

## Limitações e como foram tratadas

- A IA propôs JDBC para simplificar o SQL de Outbox/histórico. O responsável técnico preferiu uniformidade: “Prefiro JPA por uma questão de unicidade nas escolhas e mais justificavel.” A implementação foi ajustada para entidades e repositórios JPA, Persistable para inserir UUIDs atribuídos sem sobrescrever duplicatas, dirty checking na marcação e tratamento de integridade após rollback. JDBC permanece somente nas fixtures e verificações independentes dos testes.

- A primeira implementação começou antes de uma modelagem de domínio suficientemente discutida. Após revisão, foram registrados vocabulário, invariantes, casos de uso, propostas e decisões pendentes antes dos incrementos seguintes.
- A primeira proposta de eventos usava Spring Events para reduzir complexidade, embora o enunciado prefira Kafka. Após questionamento e comparação das alternativas, o responsável técnico escolheu Kafka.
- A normalização de e-mail, isoladamente, não garante unicidade sob concorrência. Foi adicionada uma restrição no banco e testes de concorrência e duplicidade, inclusive para colaboradores inativos.
- O escape dos caracteres do `LIKE` não protege sozinho contra SQL injection. A consulta usa parâmetros e foram adicionados testes com entrada maliciosa e inspeção do SQL gerado; isso não constitui uma auditoria de segurança completa.
- Os testes de PostgreSQL dependem de uma instância local já disponível. Eles exercitam um banco real, mas não são isolados por Testcontainers; a adoção de contêineres descartáveis permanece para decisão.
- A proposta inicial AFTER_COMMIT mantinha uma janela de perda após confirmar o cadastro. A Outbox resolve a gravação atômica de empregado e intenção, mas permite reenvios. Foram acrescentados testes de rollback conjunto, falha na publicação e na marcação, consumidor com falha transitória e duplicatas com Kafka real. PostgreSQL usa uma base exclusiva de testes; H2 em memória não representa durabilidade após reinício.

## Versionamento

Commits com auxílio de IA são identificados com `[IA]` e descrevem o incremento e as verificações realizadas. O versionamento começou após a base inicial de departamentos e cargos; o primeiro commit registra o estado existente, sem simular commits retroativos. O fluxo atual utiliza branches curtas a partir de `main` e integração após revisão e aprovação do responsável técnico.
