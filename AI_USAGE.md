# Uso de IA Generativa

## Ferramenta

Codex/ChatGPT auxiliou na preparação do ambiente e na implementação incremental, com o desafio e as decisões acompanhados pelo candidato.

## Trabalho realizado até agora

- A partir do enunciado, foram identificados os requisitos de domínio, arquitetura, testes e entrega.
- O Spring Initializr gerou a base Java 21/Spring Boot; o projeto foi testado com H2 e configurado para PostgreSQL local.
- O primeiro incremento separou domínio, casos de uso, adaptadores JPA e API; testes verificam validação e persistência.

## Decisões e limitações

- O domínio não depende de frameworks; o código de infraestrutura implementa as interfaces de persistência declaradas pela aplicação.
- O cargo aceita nível como texto, pois o enunciado apresenta exemplos e não especifica uma lista fechada. A unicidade de código de centro de custo também não foi presumida.
- Esta documentação é inicial e deve ser atualizada com prompts, revisões, decisões e limitações reais conforme o projeto evoluir. A implementação ainda não atende a todos os requisitos.

## Versionamento e identificação do auxílio de IA

O versionamento começou após a preparação do ambiente e o primeiro incremento de departamentos e cargos. O primeiro commit registra esse estado; não representa uma sequência de commits feita durante as etapas anteriores.

Commits com auxílio de IA usam o marcador `[IA]` no título, por exemplo: `feat: cadastra colaborador [IA]`. O corpo descreve o auxílio recebido, as decisões relevantes e as verificações efetivamente executadas. A autoria Git permanece a do candidato; auxílio de IA não implica revisão ou aprovação humana já realizada.

Cada incremento deve ter um commit coerente, incluindo seus testes e documentação quando aplicável. Decisões de modelagem podem ter commits próprios. Não registrar ferramentas instaladas, resultados de build, configurações pessoais da IDE ou segredos.
