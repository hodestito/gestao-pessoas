# 013 — Integração contínua

Estado: implementado em ci/github-actions, aguardando validação remota, revisão e aprovação de merge. Data: 28/10/2026. O PR #10 foi integrado após aprovação do responsável técnico.

## Escolha e escopo

Um workflow GitHub Actions executa a verificação em pull_request para main e push na main. O incremento cria .github/workflows/ci.yml e atualiza README, AI_USAGE e decisões 012/013; não altera classes Java, testes funcionais, migrações ou contratos.

Foi escolhido um job sequencial em Ubuntu 24.04: obter fontes, instalar Temurin 21, subir dependências, criar banco exclusivo, executar testes/build/cobertura e construir a imagem. Ele usa o Maven Wrapper e reaproveita dependências com o cache de setup-java.

PostgreSQL e Kafka são iniciados pelo mesmo docker-compose.yml usado localmente, selecionando somente esses serviços. Isso reduz duplicação de listeners, imagens e healthchecks. A alternativa de serviços declarados diretamente no workflow permitiria usar a integração nativa do runner, mas duplicaria a configuração mantida para desenvolvimento. Uma matriz H2/PostgreSQL aumentaria a validação automatizada e o tempo de execução; neste incremento, a CI prioriza PostgreSQL real com Kafka. H2 continua disponível para execução local.

## Testes e ambiente

Cada runner usa o projeto Compose gestao-ci e cria a base gestao_pessoas_test. O Maven executa clean verify com -Dkafka.tests=true, perfil postgres e URL explícita dessa base. Sem a propriedade kafka.tests, os dois testes do broker seriam ignorados; sua inclusão é intencional. Os testes controlam seus próprios tópicos e publicadores, portanto o serviço app não é iniciado junto às dependências.

O comando verifica os 87 testes existentes, empacota o JAR e gera os relatórios JaCoCo. A construção Docker ocorre somente depois do sucesso dos testes. O Dockerfile continua empacotando com -DskipTests dentro da etapa de build; a validação já ocorreu na etapa anterior. Construir a imagem não equivale a executá-la: o fluxo completo dentro de containers foi validado no incremento 012 e não foi duplicado no workflow atual.

Erros dos testes/build reprovam o job. Não foi acrescentado gate percentual de cobertura ou proteção de branch. JaCoCo continua medindo linhas/ramos sem exclusões manuais. Tornar o check obrigatório para merge é uma configuração separada do repositório, dependente de decisão do responsável técnico.

## Execuções, permissões e evidências

As actions oficiais checkout v6, setup-java v6 e upload-artifact v7 foram consultadas e fixadas pelos SHAs completos resolvidos de suas tags. O token solicita contents:read e o checkout não persiste credenciais. Não há publicação de imagem nem necessidade de secrets de infraestrutura. As credenciais PostgreSQL são exclusivas do ambiente descartável.

Novas execuções da mesma referência cancelam as anteriores. O timeout do job é de 20 minutos. Etapas always coletam logs, preservam os relatórios disponíveis e removem os volumes do runner; uma interrupção abrupta pode impedir o término dessas etapas, mas o runner hospedado é descartável.

Os artefatos Surefire/JaCoCo e logs são retidos por sete dias. Se o processo falhar antes de verify, pode não haver relatório de cobertura; logs e relatórios já existentes continuam úteis. Upload de artefatos e cache não substituem a aprovação dos testes.

GitHub Actions foi consultado no repositório: está habilitado e permite as actions utilizadas. O resultado remoto será conferido após publicar o workflow e abrir o PR; a existência do arquivo, isoladamente, não comprova execução bem-sucedida.

## Referências

- [checkout](https://github.com/actions/checkout).
- [setup-java](https://github.com/actions/setup-java).
- [upload-artifact](https://github.com/actions/upload-artifact).
- [Serviços e healthchecks Compose](https://docs.docker.com/reference/compose-file/services/).
