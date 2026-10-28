# 012 — Aplicação em Docker e ambiente Compose

Estado: integrado à main pelo PR #10 após aprovação do responsável técnico. Data: 28/10/2026. O PR #9 foi integrado após validação do responsável técnico.

## Objetivo e arquivos

Executar aplicação, PostgreSQL e Kafka com um comando, sem JDK/Maven no computador, atendendo à containerização pedida no enunciado. A execução pelo IntelliJ/Maven permanece disponível. Nenhuma classe Java ou regra de negócio foi modificada.

- Dockerfile: compilação com Maven Wrapper/JDK 21 e execução com JRE 21.
- .dockerignore: lista de inclusão restrita a Dockerfile, pom, wrapper e fontes; não envia .git, .env, target, IDE ou documentação ao contexto do build.
- docker-compose.yml: novo serviço app, dependências saudáveis, conexões internas e portas externas configuráveis.
- README, AI_USAGE e decisões 011/012: comandos, situação da evolução, alternativas e verificações.

## Imagem em duas etapas

Foi escolhido Eclipse Temurin 21 em Ubuntu Noble, com JDK na etapa build e JRE na etapa runtime. O build usa a versão de Maven fixada pelo wrapper do repositório e um cache BuildKit para /root/.m2. Somente o JAR é copiado entre as etapas. A imagem final não precisa de compilador, Maven ou fontes.

A alternativa de copiar um JAR previamente compilado seria mais curta, mas exigiria preparar o artefato no computador. Buildpacks também poderiam produzir a imagem; um Dockerfile explícito atende diretamente ao enunciado e torna as etapas visíveis para a defesa técnica.

O empacotamento usa -DskipTests. Isso compila os testes, mas não os executa; não representa validação funcional. A suíte é executada separadamente com o banco e o broker disponíveis, e o ambiente final é validado por HTTP e pelo fluxo real de eventos. Executar testes com broker no build exigiria coordenar serviços externos durante a construção da imagem.

A execução usa UID/GID 10001, sem privilégios administrativos. O ENTRYPOINT em formato exec inicia Java como processo principal, permitindo receber sinais de parada; o Compose concede até 30 segundos para encerrar. curl é disponibilizado para consultar /actuator/health no healthcheck. O endpoint verifica a saúde exposta pelo Actuator, não se todos os eventos já foram entregues.

As tags 21-jdk-noble e 21-jre-noble fixam a linha Java/distribuição, mas podem receber novos patches. Não foram fixadas por digest; builds futuros podem baixar imagens atualizadas. O cache reduz downloads, mas o primeiro build exige internet.

## Rede e inicialização

Dentro da rede Compose, app conecta em postgres:5432 e kafka:19092. localhost aponta para o próprio container, portanto a URL usada pelo Maven no Mac não serve para acessar outro serviço. As variáveis SPRING_DATASOURCE_* sobrescrevem as propriedades locais do perfil postgres; os perfis postgres,kafka são ativados no serviço app. A mesma imagem pode receber outra configuração por ambiente.

O Kafka mantém listeners separados: HOST anuncia localhost na porta publicada para clientes no computador; INTERNAL anuncia kafka:19092 para clientes na rede. O healthcheck do Kafka passou a usar o listener interno, evitando depender da porta publicada no host.

depends_on com service_healthy aguarda os healthchecks de PostgreSQL e Kafka antes de iniciar app. Isso ordena a inicialização, mas não garante disponibilidade futura das dependências. O healthcheck da aplicação tem período inicial de 60 segundos para permitir subida e migrações Flyway. Não foi acrescentada política de reinício automático; falhas permanecem visíveis no Compose.

APP_PORT, POSTGRES_PORT e KAFKA_HOST_PORT alteram portas publicadas, mantendo 8080, 5432 e 19092 internamente. Todas as publicações permanecem limitadas a 127.0.0.1. As credenciais locais gestao/gestao_local foram preservadas; não constituem configuração de produção.

## Persistência e coexistência com desenvolvimento

Os volumes postgres_data e kafka_data já existentes foram preservados. Uma execução normal usa os volumes do mesmo projeto Compose. down mantém volumes; down -v os remove. Para validar sem tocar nos dados existentes, foi escolhido o projeto gestao-docker-smoke com portas 18080, 15432 e 19094, criando rede e volumes próprios.

Subir somente postgres e kafka continua permitindo executar a aplicação pelo Maven/IntelliJ. Se app estiver ativo, é preciso pará-lo antes de executar outra instância sobre a mesma base para evitar conflito na porta e publicadores simultâneos. A arquitetura de publicação continua validada para uma instância.

## Validação executada

Docker Engine 29.9.2 e Compose 5.6.1 no Mac. O build compilou os fontes dentro da etapa Linux, baixando Maven/dependências sem reutilizar o target do Mac. A imagem foi construída com sucesso. O contexto enviado foi de 215,35 kB. A execução final confirmou Temurin JRE 21.0.12.1, UID 10001 e ausência de javac.

Verificações realizadas:

- docker compose config --quiet passou tanto com portas padrão quanto com as portas alternativas.
- O projeto isolado subiu com os três serviços saudáveis via up -d --wait.
- Um roteiro HTTP executou os seis endpoints: cadastro de departamento, cargo e colaborador, listagem, busca por departamento e inativação. Também verificou 409 por e-mail repetido, 422 por departamento inexistente e duas inativações com 204.
- A consulta direta à base exclusiva confirmou Employee INACTIVE na versão 1, dois registros na Outbox com published_at preenchido e os mesmos dois eventIds no histórico. A emissão ocorreu pelo agendamento real, com dois tópicos Kafka e consumidores ativos.
- Swagger UI, documento OpenAPI e Actuator responderam dentro da imagem final.
- O reinício de app manteve o empregado e o histórico. Depois, down sem -v seguido de up -d --wait recriou os três containers; os dados continuaram INACTIVE/versão 1, Outbox 2/publicados 2/histórico 2. Isso verificou os volumes persistentes no ciclo de remoção e recriação dos containers.
- A suíte separada clean verify com PostgreSQL na base gestao_outbox_tests_20261027_01 e Kafka real passou: 87 testes, zero falhas, erros ou ignorados, JAR gerado. Nenhum teste foi executado pelo build da imagem; são verificações distintas.

Os dados usados no roteiro pertencem apenas ao projeto gestao-docker-smoke. Após conferir a persistência, seus containers e volumes descartáveis foram removidos. Os containers e volumes de desenvolvimento não foram alterados. A validação local não representa uma configuração de produção nem validação de alta disponibilidade ou múltiplas instâncias do publicador.

## Referências

- [Build em múltiplas etapas](https://docs.docker.com/build/building/multi-stage/).
- [Serviços, portas e healthchecks do Compose](https://docs.docker.com/reference/compose-file/services/).
- [Ordem de inicialização](https://docs.docker.com/compose/how-tos/startup-order/).
- [Imagens Eclipse Temurin](https://hub.docker.com/_/eclipse-temurin).
