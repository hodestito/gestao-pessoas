# 005 — Inativação com bloqueio otimista

Estado: implementado na branch feature/employee-deactivation; aguarda revisão e aprovação de merge. Data: 27/10/2026.

## Contexto e decisão

O responsável técnico escolheu `@Version` para controlar inativações concorrentes. `DELETE /employees/{id}` preserva o cadastro e aplica `Employee.deactivate()`; retorna 204 se inativou ou já estava inativo, 404 para id não encontrado e 400 para UUID inválido.

O fluxo transporta o colaborador lido junto com a versão esperada em `VersionedEmployee`. O adaptador JPA reconstitui `EmployeeEntity` com essa versão; Hibernate verifica a versão na gravação. A migração V3 adiciona a coluna `version` com valor inicial 0 aos registros existentes. A versão não é exposta na API.

## Transações e conflitos

Cada tentativa ocorre por um componente Spring separado com `@Transactional(REQUIRES_NEW)`. Se a primeira gravação falhar por conflito otimista, sua transação termina com rollback e o coordenador tenta uma vez em outra transação, relendo o registro. Se já estiver inativo, não há nova gravação. Um segundo conflito é propagado e retorna 409.

`REQUIRES_NEW` cria transações independentes: se esse fluxo vier a ser chamado de uma transação externa, a operação interna pode confirmar independentemente dela. O desenho de transação e eventos deve ser revisto quando Kafka for integrado.

## Alternativas e limitações

Atualização condicional `UPDATE ... WHERE status = 'ACTIVE'` poderia informar o número de linhas alteradas atomicamente, mas colocaria a condição de transição também na consulta SQL. Bloqueio pessimista poderia serializar a leitura com lock, ao custo de espera e risco de contenção. O controle otimista foi escolhido para manter a transição no domínio e detectar versões obsoletas.

O controle só protege operações que verifiquem e incrementem a versão. Escritas SQL externas que não respeitem o mecanismo podem contorná-lo. A releitura é limitada a uma tentativa; conflitos persistentes não são repetidos indefinidamente.

O evento `EmployeeStatusChangedEvent` e sua persistência/publicação ainda não existem. Controle otimista não garante entrega ao Kafka nem a atomicidade entre banco e broker; Outbox ou outra estratégia ainda será discutida.

## Verificação

A suíte completa passou com 44 testes em H2 e PostgreSQL 16 local. Depois foi acrescentado um teste de duas requisições HTTP concorrentes; os quatro testes de integração de inativação/concorrência foram repetidos nos dois bancos sem falhas. Um teste com duas transações coordenadas força ambas a lerem a mesma versão e verifica que a segunda gravação é rejeitada. O teste unitário do coordenador simula o conflito para verificar a releitura limitada; não força essa corrida através de uma chamada HTTP real. Testcontainers ainda não foi adotado.
