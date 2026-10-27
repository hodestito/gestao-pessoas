# Casos de uso

Atualizado em 27/10/2026. Complementa o [modelo de domínio](domain.md). Contratos propostos ainda não são comportamento disponível na API.

## Visão geral

| ID | Caso de uso | Endpoint obrigatório | Situação |
|---|---|---|---|
| UC-01 | Criar departamento | POST /departments | Implementado, sujeito à revisão do modelo. |
| UC-02 | Criar cargo | POST /positions | Implementado, sujeito à revisão do modelo. |
| UC-03 | Cadastrar colaborador | POST /employees | Em especificação. |
| UC-04 | Listar colaboradores | GET /employees | Em especificação. |
| UC-05 | Buscar por departamento | GET /employees/search?department=RH | Em especificação. |
| UC-06 | Inativar colaborador | DELETE /employees/{id} | Em especificação. |

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

**Entrada proposta:** name, email, departmentId e positionId. O sistema gera id e createdAt; ACTIVE como status inicial é uma proposta.

**Regras confirmadas:** departamento e cargo devem existir; e-mail é único mesmo entre inativos, comparado sem diferenciar caixa e com espaços das extremidades removidos; nome tem até 120 caracteres. Criar Employee como ACTIVE foi aprovado para o modelo inicial.

**Fluxo proposto:**

1. Validar a estrutura da requisição e os valores do domínio.
2. Consultar as referências de departamento e cargo.
3. Verificar disponibilidade do e-mail para antecipar uma resposta compreensível.
4. Criar Employee e salvar em uma transação protegida por unicidade e chaves estrangeiras no banco.
5. Produzir EmployeeCreatedEvent por Kafka. O mecanismo e suas garantias serão definidos no incremento de eventos.
6. Retornar 201 com os dados do colaborador criado.

**Erros HTTP propostos:** 400 para valores/identificadores malformados; 422 para referências inexistentes; 409 para e-mail duplicado. Esses códigos são escolhas de contrato, não exigências expressas do enunciado. Uma falha não prevista de infraestrutura deve ser distinguida dos erros de negócio.

**Cenários de aceitação a implementar:**

- Dados válidos e referências existentes criam um colaborador e produzem o evento exigido.
- Departamento inexistente impede o cadastro, sem colaborador nem evento de criação confirmado.
- Cargo inexistente impede o cadastro.
- Duas requisições simultâneas com o mesmo e-mail produzem somente um cadastro; a outra recebe conflito.
- Rollback da gravação impede o consumo de um evento de criação confirmada.

**Limite da garantia:** a resposta de criação não garante que o histórico assíncrono já tenha sido gravado. Sem uma estratégia como Outbox, existe uma janela de perda entre a gravação e a publicação no Kafka.

## UC-04 — Listar colaboradores

**Proposta de contrato:** retornar 200 com uma página de colaboradores, incluindo ativos e inativos. Sem dados, retornar coleção vazia. Definir tamanho padrão, máximo e ordenação estável antes de implementar.

**Dados previstos:** id, name, email, departmentId, positionId, createdAt e status. Evitar expor entidades JPA diretamente no contrato HTTP.

**Erros propostos:** 400 para parâmetros de paginação inválidos.

**Cenários a implementar:** coleção vazia, presença dos estados esperados, limites de página e ordenação estável para registros com valores de ordenação iguais. Listagem não produz evento de negócio.

## UC-05 — Buscar colaboradores por departamento

**Entrada obrigatória pelo exemplo do enunciado:** nome do departamento no parâmetro department. O exemplo RH não define sozinho se a comparação é parcial, exata ou sensível a caixa.

**Proposta:** correspondência exata sem distinguir maiúsculas/minúsculas, incluindo ativos/inativos e com paginação. Sem correspondências, retornar 200 com coleção vazia. Definir o comportamento de nomes repetidos antes de implementar, pois o cadastro atual permite repetições.

**Erros propostos:** 400 para parâmetro ausente/vazio ou paginação inválida.

**Cenários a implementar:** encontrar somente colaboradores dos departamentos correspondentes, não incluir outros departamentos, retornar vazio quando não houver correspondência e respeitar a comparação/paginação definidas. A busca não produz evento de negócio.

## UC-06 — Inativar colaborador

**Regra confirmada:** inativo representa pessoa que saiu da empresa. Preservar o registro ao atender DELETE é uma proposta coerente com o evento de mudança de status pedido pelo enunciado; ainda será revisada com o candidato.

**Fluxo proposto:** localizar Employee; executar seu comportamento de inativação; persistir a mudança em uma transação; produzir EmployeeStatusChangedEvent após uma transição efetiva; retornar 204.

**Tratamento proposto de repetição:** se já estiver inativo, retornar 204 e não produzir outro evento de mudança. Identificador malformado retorna 400; inexistente retorna 404.

**Concorrência:** duas inativações simultâneas precisam ser tratadas para não confirmar duas transições/eventos para o mesmo desligamento. Escolher a estratégia de concorrência ao implementar e cobri-la com teste.

**Cenários a implementar:** ativo torna-se inativo mantendo seus dados; inexistente retorna erro; repetição não duplica mudança nem evento; rollback não confirma desligamento; falha do consumidor não transforma cadastro já inativado em ativo.

## Consumo assíncrono transversal

O enunciado exige consumidores para EmployeeCreatedEvent e EmployeeStatusChangedEvent, com logs customizados e atualização de employee_events.

**Decisão:** usar Kafka; publicação e consumo ainda não implementados. Definir transações, identificação do evento, conteúdo mínimo, proteção contra duplicatas, observação de falhas e tempo de espera dos testes no incremento de eventos. Logs devem evitar expor dados pessoais desnecessários.

**Critérios:** testar publicação e consumo, verificar o histórico após processamento e usar espera limitada nos testes. Documentar o comportamento em falhas e a janela de perda sem Outbox. Não afirmar sucesso de consumo apenas porque a API respondeu 201 ou 204.

## Como revisar cada incremento

Antes do código, revisar regras confirmadas e propostas do caso de uso. Depois, percorrer requisição → interface HTTP → aplicação → domínio → persistência e, quando existir, consumo do evento.

O incremento termina com evidências para três perguntas: o que mudou, por que foi feito assim e como foi verificado. Atualizar os documentos afetados e AI_USAGE.md e registrar um commit coerente com o marcador [IA].
