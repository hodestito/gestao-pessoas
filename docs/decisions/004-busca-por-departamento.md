# 004 — Busca de colaboradores por departamento

Estado: implementação revisada e integração à main autorizada pelo responsável técnico. Data: 27/10/2026.

## Contexto e decisão proposta

O enunciado exige `GET /employees/search?department=RH`, sem definir se a busca é parcial nem o tratamento de nomes repetidos. Foi implementada a correspondência exata, sem diferenciar caixa, depois de remover espaços nas extremidades. Nomes de departamento não são únicos no modelo atual: todos os departamentos homônimos são incluídos, com colaboradores ativos e inativos.

Parâmetro ausente/vazio ou paginação inválida retorna 400; departamento sem correspondência retorna 200 com página vazia. As respostas usam PageResult e o mesmo contrato, limites e ordenação da listagem geral, conforme revisão na decisão 009.

## Implementação e alternativas

`SearchEmployeesByDepartment` é um caso de uso e `EmployeeRepository` declara a consulta. `EmployeePagination` centraliza parâmetros e limites; o contrato exige ordenação por createdAt/id, aplicada pelo adaptador JPA. `EmployeeJpaRepository` usa JPQL com parâmetro nomeado e subconsulta `exists`, relacionando id do departamento ao identificador salvo no colaborador. Spring Data fica na infraestrutura, conforme decisão 009.

Alternativas: exigir nomes de departamento únicos e consultar um único departamento; ou buscar por id de departamento. Ambas exigem alteração de contrato ou regra de negócio. A subconsulta considera todos os nomes coincidentes sem multiplicar colaboradores por joins acidentais.

## Limitações e verificação

Busca sem distinção de caixa usa `lower`; comportamento com acentos e collation depende do banco, e não prometemos equivalência entre acentuadas e não acentuadas. A consulta pode exigir contagem para Page e um índice apropriado para volume elevado; não houve benchmark. A API ainda não tem autenticação/autorização.

Testes verificam comparação exata e caixa, departamentos homônimos, ativos/inativos, paginação, ausência, entradas inválidas e um padrão de SQL injection como parâmetro. Suíte completa: 36 testes sem falhas em H2 e 36 em PostgreSQL 16 local. Não houve Testcontainers.
