# Casos de uso

Atualizado em 28/10/2026. Descreve os seis fluxos implementados, complementando o [modelo de domínio](domain.md) e a [arquitetura](architecture.md). Os critérios específicos foram definidos com o candidato quando o enunciado não os determinava.

## Visão geral

| ID | Caso de uso | Endpoint obrigatório | Situação |
|---|---|---|---|
| UC-01 | Criar departamento | POST /departments | Implementado e integrado à main. |
| UC-02 | Criar cargo | POST /positions | Implementado e integrado à main. |
| UC-03 | Cadastrar colaborador | POST /employees | Cadastro, política de domínio, Outbox/Kafka e histórico integrados à main. |
| UC-04 | Listar colaboradores | GET /employees | Implementado; integração aprovada pelo responsável técnico. |
| UC-05 | Buscar por departamento | GET /employees/search?department=RH | Implementado, com integração autorizada pelo responsável técnico. |
| UC-06 | Inativar colaborador | DELETE /employees/{id} | API e evento integrados à main pelos PRs #3 e #5. |

O solicitante é um consumidor da API de Gestão de Pessoas. Não há mecanismo de autenticação/autorização implementado. O enunciado não define papéis de acesso.

## UC-01 — Criar departamento

**Entrada:** name e costCenterCode.

**Fluxo implementado:** a API valida a entrada; o caso de uso cria Department; o domínio remove espaços nas extremidades e valida os valores; o adaptador JPA persiste; a API retorna 201 com id, name e costCenterCode.

**Erros implementados:** campo ausente, vazio ou acima do limite de entrada resulta em 400. Os limites iniciais são 120 caracteres para nome e 50 para código; não foram derivados de uma regra confirmada pelo candidato.

**Consistência:** gravação de um departamento na transação do repositório. Não há evento de departamento exigido neste escopo.

**Exemplo:** cadastrar RH / CC-01 retorna um identificador. Um nome em branco é rejeitado. Não há regra implementada de unicidade do nome ou do código.

**Evidência existente:** testes de domínio/aplicação e teste HTTP com consulta ao banco. O incremento foi validado com H2 e PostgreSQL antes desta documentação.

## UC-02 — Criar cargo

**Entrada:** title e level.

**Fluxo implementado:** validar a entrada, criar Position, persistir via JPA e retornar 201 com identificador e dados.

**Erros implementados:** campos ausentes, vazios ou acima dos limites de entrada resultam em 400. Os limites iniciais são 120 caracteres para título e 50 para nível. Não há enumeração fechada dos níveis.

**Consistência:** gravação de um cargo na transação do repositório. Não há evento de cargo exigido neste escopo.

**Exemplo:** Engenheiro / Sênior é aceito; nível em branco é rejeitado.

**Evidência existente:** testes de domínio/aplicação e teste HTTP com consulta ao banco, validados com H2 e PostgreSQL no incremento anterior.

## UC-03 — Cadastrar colaborador

**Entrada implementada:** name, email, departmentId e positionId. O sistema gera id e createdAt; o estado inicial é ACTIVE.

**Regras confirmadas:** departamento e cargo devem existir; e-mail é único mesmo entre inativos, comparado sem diferenciar caixa e com espaços das extremidades removidos; nome tem até 120 caracteres. Criar Employee como ACTIVE foi aprovado para o modelo inicial.

**Fluxo implementado:**

1. Validar a estrutura da requisição e os valores do domínio.
2. Delegar a EmployeeRegistrationPolicy, que consulta existência de departamento e cargo pela porta do domínio.
3. A política verifica disponibilidade do e-mail para antecipar uma resposta compreensível; mantém a ordem departamento → cargo → e-mail.
4. Criar Employee e salvar em uma transação protegida por unicidade e chaves estrangeiras no banco.
5. Gravar EmployeeCreatedEvent na Outbox na mesma transação; retornar 201 com os dados. Um publicador periódico envia ao Kafka e um consumidor grava o histórico de forma idempotente por eventId.

**Erros HTTP implementados:** 400 para valores/identificadores malformados; 422 para referências inexistentes; 409 para e-mail duplicado. Esses códigos foram aprovados pelo responsável técnico, não são exigências expressas do enunciado. Violações inesperadas de integridade não são transformadas em erros de negócio conhecidos.

**Cenários verificados:**

- Dados válidos e referências existentes criam colaborador e evento na mesma transação; publicação e consumo são assíncronos.
- Departamento inexistente impede o cadastro, sem colaborador nem evento de criação confirmado.
- Cargo inexistente impede o cadastro.
- Duas requisições simultâneas com o mesmo e-mail produzem somente um cadastro; a outra recebe conflito.
- Erro no banco após os inserts desfaz colaborador e evento; publicação com falha mantém pendência e permite reenvio.

**Limite da garantia:** a resposta de criação confirma empregado e intenção de publicação, sem garantir que o histórico já exista. Outbox evita perder a intenção após o commit; reenvios continuam possíveis e são deduplicados no histórico. Kafka precisa estar habilitado e disponível para o processamento avançar. Consultar a decisão 006.

## UC-04 — Listar colaboradores

**Contrato implementado:** retornar 200 com uma página de colaboradores, incluindo ativos e inativos quando status não é informado. Sem dados, retornar coleção vazia. `page` começa em 0; `size` varia de 1 a 100 e tem padrão 20. Ordenação crescente por createdAt e depois id. A ordenação é determinística para o conjunto consultado, mas alterações entre requisições podem deslocar itens entre páginas.

**Dados retornados:** id, name, email, departmentId, positionId, createdAt e status. A resposta possui content, page, size, totalElements e totalPages; não expõe entidades JPA nem depende do formato JSON padrão de Page. A aplicação usa PageResult e EmployeePagination próprios; o adaptador JPA concentra a conversão para Spring Data.

**Filtros:** name opcional, parcial, sem diferenciar caixa e sem garantia de ignorar acentos; valores vazios ou só com espaços não filtram. `%`, `_` e `!` são tratados literalmente. status opcional aceita ACTIVE ou INACTIVE. Não há filtro por id neste incremento.

**Erros:** 400 para parâmetros de paginação inválidos, status não reconhecido ou deslocamento maior que o limite suportado. EmployeePagination valida tamanho e deslocamento ao ser criado pelo caso de uso, inclusive por construção direta.

**Cenários testados:** coleção vazia, combinação de nome e status, páginas e desempate por id, caracteres literais de busca, limites e entradas inválidas. Listagem não produz evento de negócio.

## UC-05 — Buscar colaboradores por departamento

**Entrada obrigatória pelo exemplo do enunciado:** nome do departamento no parâmetro department. O exemplo RH não define sozinho se a comparação é parcial, exata ou sensível a caixa.

**Comportamento implementado:** correspondência exata sem distinguir maiúsculas/minúsculas, incluindo ativos/inativos e com paginação. Espaços nas extremidades são removidos; acentos não são ignorados intencionalmente. Sem correspondências, retornar 200 com coleção vazia. Nomes repetidos identificam todos os departamentos correspondentes, pois o cadastro atual permite repetições.

**Erros:** 400 para parâmetro ausente/vazio ou paginação inválida.

**Cenários testados:** encontrar colaboradores dos departamentos correspondentes inclusive homônimos e inativos, não incluir outros departamentos, retornar vazio sem correspondência, respeitar comparação/paginação e tratar entrada com sintaxe de SQL injection como dado. A busca não produz evento de negócio.

## UC-06 — Inativar colaborador

**Regra confirmada:** inativo representa pessoa que saiu da empresa. DELETE preserva o registro e altera o estado de ativo para inativo.

**Fluxo implementado:** localizar Employee e sua versão; executar o comportamento de inativação; persistir a mudança e EmployeeStatusChangedEvent na mesma transação e retornar 204. O publicador envia ao tópico employee-status-changed.v1; o consumidor grava o histórico e logs. O evento inclui estado anterior/novo e instante da mudança.

**Tratamento de repetição:** se já estiver inativo, retornar 204 sem nova gravação nem outro evento lógico. Identificador malformado retorna 400; inexistente retorna 404, sem evento de inativação.

**Concorrência:** o responsável técnico escolheu bloqueio otimista com `@Version`. Uma alteração com versão antiga falha antes do append da Outbox; após rollback, é feita uma nova tentativa em outra transação. Se já estiver inativo, não grava evento. Se ocorrer novo conflito, a API retorna 409. O controle depende de escritores que respeitem e atualizem a versão. Outbox resolve a atomicidade da mudança com a intenção de publicação; o consumidor trata reenvios pela chave do evento.

**Cenários testados:** ativo torna-se inativo mantendo seus dados; inexistente retorna erro; repetição não faz nova gravação; duas requisições simultâneas retornam 204 com uma alteração e um evento lógico; uma gravação com versão antiga é rejeitada. Falha simulada na persistência da Outbox e falha injetada após as gravações desfazem status, versão e evento. Kafka real verifica consumo, falha transitória, duplicata com offset confirmado e inativação processada antes da criação. Não se afirma que o teste HTTP força deterministicamente ambas as requisições a lerem a mesma versão.

## Consumo assíncrono transversal

O enunciado exige consumidores para EmployeeCreatedEvent e EmployeeStatusChangedEvent, com logs customizados e atualização de employee_events.

**Decisão:** Kafka com Outbox para EmployeeCreatedEvent e EmployeeStatusChangedEvent. Dois tópicos aprovados pelo responsável técnico preservam o tópico de criação. eventId identifica a ocorrência lógica e é preservado nos reenvios; histórico tem chave única por evento. O payload inclui versão do contrato, tipo, identificadores e instante; inativação também inclui previousStatus/newStatus. Não há nome/e-mail. Logs usam tipo, tópico e identificadores.

**Critérios verificados:** publicação e consumo dos dois eventos com Kafka real, histórico após processamento, espera limitada, falha transitória e duplicata com offset confirmado. O teste de publicação simula falha na marcação após envio; não reinicia o processo. Há reenvios, uma instância validada, sem ordenação entre tópicos, retenção automática ou DLQ. O histórico aceita inativação antes da criação e não altera o agregado. Não afirmar sucesso de consumo apenas porque a API respondeu 201 ou 204.

## Como revisar cada incremento

Antes do código, revisar regras confirmadas e propostas do caso de uso. Depois, percorrer requisição → interface HTTP → aplicação → domínio → persistência e, quando existir, consumo do evento.

O incremento termina com evidências para três perguntas: o que mudou, por que foi feito assim e como foi verificado. Atualizar os documentos afetados e AI_USAGE.md e registrar um commit coerente com o marcador [IA].
