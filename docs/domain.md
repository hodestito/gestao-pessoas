# Domínio: Gestão de Pessoas

Atualizado em 27/10/2026. Documento de trabalho para orientar a implementação e a defesa técnica.

## Como ler este documento

- **Enunciado:** requisito expresso do desafio.
- **Confirmado pelo candidato:** regra de negócio definida na discussão.
- **Proposta:** escolha sugerida, ainda sujeita à revisão.
- **Implementado:** comportamento presente no código; não significa que todo o modelo esteja concluído.

Cada mudança de entendimento deve atualizar este documento e os casos de uso no mesmo incremento. O histórico do Git registra a evolução; este texto apresenta o entendimento atual.

## Contexto e linguagem compartilhada

O serviço atende ao time de Gente & Gestão para cadastrar colaboradores, departamentos e cargos e produzir eventos sobre criação e inativação de colaboradores.

| Termo | Significado | Origem |
|---|---|---|
| Colaborador / Employee | Pessoa cadastrada no serviço. | Enunciado |
| Colaborador inativo | Pessoa que saiu da empresa. | Confirmado pelo candidato |
| Cargo / posição / Position | Termos sinônimos; usamos cargo na explicação e Position no código. | Confirmado pelo candidato |
| Departamento / Department | Unidade organizacional à qual o colaborador está vinculado. | Enunciado |
| Centro de custo | Código associado ao departamento; formato e unicidade não foram definidos pelo negócio. | Enunciado e questão aberta |
| Histórico de eventos | Registro produzido pelo consumidor das mudanças de colaboradores. Não equivale ao log técnico. | Enunciado |

## Escopo e limites

São obrigatórios os seis casos de uso descritos em [use-cases.md](use-cases.md), os dois eventos de colaboradores e seus consumidores para logs e histórico.

**Proposta:** um único contexto delimitado de Gestão de Pessoas, implementado em uma aplicação. Não há necessidade demonstrada de separar departamentos, cargos e colaboradores em serviços independentes.

Transferência, reativação, recontratação, edição de cadastro, exclusão de departamentos/cargos e autenticação não aparecem nos endpoints obrigatórios. Ficam fora do escopo inicial proposto, sem afirmar que sejam desnecessários em um produto real.

## Regras confirmadas

| Regra | Origem | Onde deverá ser garantida |
|---|---|---|
| Não cadastrar colaborador em departamento inexistente. | Candidato | Caso de uso verifica existência; banco protege a referência. |
| Não cadastrar colaborador em cargo inexistente. | Candidato | Caso de uso verifica existência; banco protege a referência. |
| E-mail de colaborador deve ser único, inclusive para inativos. | Candidato | Caso de uso pode antecipar o erro; restrição no banco garante a regra sob concorrência. |
| Inativação representa saída da empresa. | Candidato | Comportamento do agregado Employee. |
| Criar colaborador produz EmployeeCreatedEvent. | Enunciado | Fluxo de cadastro e publicação de eventos. |
| Inativar colaborador produz EmployeeStatusChangedEvent. | Enunciado | Mudança de estado e publicação de eventos. |
| Consumidores geram logs e atualizam uma tabela de histórico. | Enunciado | Consumidores de eventos e persistência do histórico. |

O cadastro verifica existência e unicidade e persiste colaborador e EmployeeCreatedEvent na mesma transação, usando Outbox. As constraints garantem unicidade e referências; a inativação está integrada à main e usa controle otimista. Nesta branch, Kafka e histórico do cadastro estão implementados; o evento de inativação permanece pendente.

## Agregados e invariantes: proposta de modelagem

Uma **invariante** é uma regra que precisa continuar verdadeira após uma operação válida. Um **agregado** define um limite de consistência e tem uma raiz pela qual suas mudanças são realizadas. Não é necessário agrupar toda a estrutura organizacional em um objeto.

| Raiz proposta | Responsabilidade | Referências |
|---|---|---|
| Employee | Proteger os dados e as mudanças de estado do colaborador. | departmentId e positionId. |
| Department | Proteger os dados do departamento. | Não contém uma coleção de todos os colaboradores. |
| Position | Proteger os dados do cargo. | Não contém uma coleção de todos os colaboradores. |

Employee foi iniciado como classe com identidade pelo id, criação ACTIVE e comportamento de inativação. Department e Position já existem como registros imutáveis com validações de construção; isso, isoladamente, não demonstra todo o DDD exigido pelo desafio.

### Quem protege cada tipo de regra

- **Dentro de Employee:** validade dos seus valores e transições permitidas de estado.
- **Na aplicação:** coordenar a consulta a departamento/cargo e a gravação do colaborador.
- **No banco:** impedir e-mails duplicados e referências inválidas mesmo com requisições simultâneas.

Por exemplo, consultar um e-mail antes de gravar não resolve a concorrência: duas requisições podem consultar ao mesmo tempo e ambas encontrarem o e-mail disponível. A restrição de unicidade decide a gravação, e a API deve converter a violação esperada em erro de negócio.

### Objetos de valor e serviços de domínio

CostCenterCode e Email são objetos de valor. Email remove espaços nas extremidades e converte para minúsculas com `Locale.ROOT`, para a comparação aprovada pelo candidato. O formato implementado é uma política simples com limite técnico de 254 caracteres; não cobre todos os endereços possíveis nas RFCs nem comprova que o endereço exista. A unicidade entre colaboradores é protegida por constraint no banco; outra constraint exige que o valor gravado esteja em minúsculas e sem espaços externos.

O enunciado exige serviços de domínio. Ainda precisamos identificar e justificar qual regra de negócio, independente de frameworks, deve ser expressa por esse serviço. Os casos de uso existentes são serviços de aplicação; não devem ser apresentados como serviços de domínio apenas para cumprir a nomenclatura.

## Propostas que precisam de revisão

| Tema | Proposta inicial | Questão ainda aberta |
|---|---|---|
| DELETE | Integrado pelo PR #3; inativa e preserva o registro; retorna 204 para alteração ou repetição e 404 para id inexistente. | Estender Outbox para a mudança de estado. |
| Inativação repetida | O domínio retorna false quando não há mudança; a API responde 204 sem nova gravação. | A garantia de evento não duplicado depende do futuro desenho de publicação e consumo. |
| Listagem | Implementada com filtro opcional por nome e status, ativos e inativos por padrão e paginação. | Avaliar desempenho e estabilidade entre requisições conforme o volume de dados. |
| Busca por departamento | Implementada com nome exato sem distinguir caixa, paginação, todos os departamentos homônimos e lista vazia quando não há correspondência. | Avaliar requisitos de acentuação e desempenho conforme o uso. |

O limite de 120 caracteres para o nome de Employee foi aprovado pelo candidato. Limites de nome de departamento, centro de custo, título e nível são decisões técnicas iniciais, ainda não confirmadas como regras de negócio. Nível do cargo está como texto, sem lista fechada, e o código não exige unicidade de nome de departamento ou centro de custo.

## Transações e eventos

O candidato aprovou Kafka e posteriormente solicitou Outbox. No cadastro, a aplicação registra EmployeeCreatedEvent pela porta EmployeeEventOutbox, na mesma transação do empregado. Um publicador periódico envia ao Kafka após o commit; o consumidor grava o histórico de forma idempotente por eventId. O responsável técnico escolheu JPA para toda a persistência da aplicação. A decisão 006 detalha classes, contrato, transações, testes e limites. A inativação ainda precisa dessa integração.

Kafka não torna a transação do banco e a publicação atômicas. Sem uma estratégia adicional, a aplicação pode confirmar o cadastro e falhar antes de publicar o evento. A Outbox permite registrar a intenção de publicação na mesma transação do colaborador, com mecanismo posterior de envio; também exige cuidados com duplicatas e operação. Não prometer exactly-once.

## Situação do trabalho de DDD

1. Problema e requisitos: lidos e identificados.
2. Linguagem compartilhada: iniciada, com definições confirmadas de inativo e cargo.
3. Regras e cenários: em discussão; consultar a tabela de propostas.
4. Contexto e agregados: propostos neste documento, ainda sujeitos à revisão.
5. Modelo de Employee: criação, cadastro, listagem, busca por departamento e inativação pela API implementados; evento de criação com Outbox/Kafka nesta branch; evento de inativação pendente.
6. Validação do modelo com testes: domínio, aplicação, integração HTTP/banco, concorrência e evento de criação. Testes de evento de inativação e avaliação da cobertura permanecem pendentes.

Para explicar este passo: quais regras estão confirmadas, quais são propostas e por que uma regra pode precisar da entidade, do caso de uso e do banco trabalhando juntos?
