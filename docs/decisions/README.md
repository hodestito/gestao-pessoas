# Decisões arquiteturais

As decisões 001–014 foram incorporadas ao projeto. As ADRs registram o contexto de cada incremento; contagens de testes e limitações nelas descritas pertencem àquela revisão. Para o estado consolidado, consulte [arquitetura](../architecture.md), [domínio](../domain.md) e [entrega](../delivery.md).

| ADR | Decisão | Evolução relevante |
|---|---|---|
| [001](001-employee-e-kafka.md) | Identidade de Employee e direção para Kafka | Outbox detalhada em 006 |
| [002](002-cadastro-employee.md) | Cadastro transacional e constraints | Política extraída em 008 |
| [003](003-listagem-colaboradores.md) | Listagem, filtros e paginação | Tipos próprios em 009 |
| [004](004-busca-por-departamento.md) | Correspondência exata, caixa e homônimos | Contrato preservado |
| [005](005-inativacao-version.md) | Inativação com controle otimista | Evento incluído em 007 |
| [006](006-outbox-cadastro.md) | Outbox, Kafka e histórico idempotente | Segundo contrato em 007; testes descartáveis em 014 |
| [007](007-evento-inativacao.md) | Dois tópicos e evento de mudança de status | Ausência de ordem entre tópicos preservada |
| [008](008-servico-dominio-cadastro.md) | Serviço de domínio para admissão | Independente de frameworks |
| [009](009-nucleo-independente-e-identidade.md) | Paginação própria e igualdade por ID | Domínio/aplicação somente Java |
| [010](010-cobertura-e-caminhos-criticos.md) | JaCoCo e testes de falha | Medições posteriores incluem OpenAPI |
| [011](011-openapi-swagger.md) | Contrato HTTP e Swagger UI | Dois formatos de erro existentes documentados |
| [012](012-docker-compose.md) | Imagem JDK/JRE e ambiente Compose | Usado para executar a aplicação |
| [013](013-integracao-continua.md) | GitHub Actions e evidências | Preparação de serviços substituída em 014 |
| [014](014-testcontainers.md) | PostgreSQL/Kafka descartáveis nos testes | Modo completo atual da CI |

O registro histórico de Testcontainers adiado nas etapas iniciais não descreve mais a configuração atual. Também não há um novo requisito de negócio apenas porque uma evolução foi sugerida em uma ADR. A rastreabilidade dos commits assistidos por IA está em [AI_USAGE.md](../../AI_USAGE.md).
