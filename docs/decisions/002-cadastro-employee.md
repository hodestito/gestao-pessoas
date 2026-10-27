# 002 — Cadastro de colaborador

Estado: cadastro aprovado e implementado; complemento de Outbox/Kafka para revisão descrito na decisão 006. Data: 27/10/2026.

## Decisões

- Cadastro aceita name, email, departmentId e positionId, cria identificador UUID e usa `Clock` para obter createdAt. Nos testes de aplicação, UUID e relógio são fornecidos com valores fixos.
- O caso de uso é Java puro e verifica existência das referências e e-mail. Um serviço na infraestrutura aplica `@Transactional` à operação inteira; a API depende da porta de entrada. Alternativa: transações individuais em cada repositório, que não definem a operação como uma unidade.
- A consulta prévia de e-mail permite resposta clara. O banco aplica unicidade sobre e-mail normalizado, exige caixa baixa e ausência de espaços externos e protege referências com chaves estrangeiras. A consulta sozinha não evita corrida.
- E-mail duplicado retorna 409; departamento/cargo inexistentes retornam 422; valores inválidos retornam 400. Os códigos foram aprovados pelo responsável técnico.
- Persistência e domínio são objetos separados. Employee.restore reconstitui estado persistido sem fingir que houve uma nova contratação.

## Consequências e limites

O adaptador usa `saveAndFlush` para antecipar a execução do SQL; flush não é commit. O commit acontece ao fim do método transacional. O tratamento de conflitos de corrida identifica constraints conhecidas por seus nomes nas exceções; é dependente das mensagens dos bancos e deve ser revisitado se mudarem provedor ou dialeto. Falhas inesperadas de integridade não são apresentadas como conflito conhecido.

Uma constraint única em e-mail normalizado cobre ativos e inativos. Testes concorrentes exercitam a API; um teste adicional confirma que o próprio banco rejeita duplicação. Foram executados testes no H2 e no PostgreSQL 16 local. Não foi usado Testcontainers.

O complemento descrito na decisão 006 grava EmployeeCreatedEvent na Outbox na mesma transação do cadastro. A resposta 201 confirma empregado e intenção; não implica publicação Kafka nem processamento do histórico já concluídos.
