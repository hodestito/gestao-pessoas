# 003 — Listagem paginada de colaboradores

Estado: implementado na branch feature/employee-list, aguardando revisão e aprovação antes do merge. Data: 27/10/2026.

## Contexto e decisões

- O responsável técnico escolheu `Page` do Spring Data. A aplicação usa Page e Pageable, aceitando dependência do Spring Data nessa camada; o domínio permanece independente de frameworks.
- O endpoint GET /employees retorna JSON próprio com content, page, size, totalElements e totalPages. Isso evita depender diretamente da serialização automática de Page.
- Como proposto para esta etapa, filtros opcionais por nome e status são combinados; sem status, ativos e inativos são incluídos. Nome é parcial e não distingue caixa; espaços externos são removidos e uma entrada vazia não filtra. Não há garantia de equivalência entre letras com e sem acento.
- Páginas começam em 0, com tamanho padrão 20 e máximo 100; valores inválidos ou deslocamento maior que Integer.MAX_VALUE retornam 400. Ordenação é ascendente por createdAt e id para desempate.
- O adaptador JPA usa Specification para construir apenas os predicados necessários. Os caracteres de SQL LIKE (`%`, `_` e o caractere de escape `!`) são escapados, para serem interpretados como texto fornecido pelo usuário.

## Alternativas e consequências

Uma interface própria de paginação eliminaria a dependência de Spring Data na aplicação, ao custo de mais conversões. Paginação por cursor pode ser apropriada para grandes volumes ou navegação diante de alterações, mas exige outro contrato. Paginação por número de página pode mudar de resultado quando registros são incluídos ou removidos entre requisições; ordenação determinística não produz um snapshot entre requisições.

Page pode exigir consulta de contagem para totalElements e totalPages. Busca parcial com `LIKE '%texto%'` e `LOWER` pode ser cara em volume alto e não deve ser considerada indexada/eficiente sem medição; avaliar EXPLAIN e alternativas conforme volume e padrões de busca. Retornar e-mail na listagem respeita o modelo atual, mas autenticação/autorização e minimização de dados ainda precisam ser definidos.

## Verificação

A suíte completa passou em H2 e PostgreSQL local com 29 testes em cada execução. Após o ajuste de limite de deslocamento, os sete testes da listagem foram executados novamente nos dois bancos, sem falhas. Não houve Testcontainers ou teste de carga.
