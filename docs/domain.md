# Domínio: Gestão de Pessoas

Atualizado em 28/10/2026. Documento de trabalho para orientar a implementação e a defesa técnica.

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

O cadastro verifica existência e unicidade e persiste colaborador e EmployeeCreatedEvent na mesma transação, usando Outbox. As constraints garantem unicidade e referências; a inativação está integrada à main e usa controle otimista. Nesta branch, a transição também grava EmployeeStatusChangedEvent na mesma transação; Kafka e histórico atendem aos dois tipos.

## Agregados e invariantes: proposta de modelagem

Uma **invariante** é uma regra que precisa continuar verdadeira após uma operação válida. Um **agregado** define um limite de consistência e tem uma raiz pela qual suas mudanças são realizadas. Não é necessário agrupar toda a estrutura organizacional em um objeto.

| Raiz proposta | Responsabilidade | Referências |
|---|---|---|
| Employee | Proteger os dados e as mudanças de estado do colaborador. | departmentId e positionId. |
| Department | Proteger os dados do departamento. | Não contém uma coleção de todos os colaboradores. |
| Position | Proteger os dados do cargo. | Não contém uma coleção de todos os colaboradores. |

Employee, Department e Position são classes finais com identidade pelo ID dentro do próprio tipo. Employee protege criação ACTIVE e inativação; Department/Position mantêm dados imutáveis e validações de construção. Duas representações da mesma entidade com atributos descritivos diferentes continuam iguais se o ID for o mesmo. Objetos de valor, como Email e CostCenterCode, continuam comparados pelo valor.

### Quem protege cada tipo de regra

- **Dentro de Employee:** validade dos seus valores e transições permitidas de estado.
- **No serviço de domínio EmployeeRegistrationPolicy:** aplicar as condições de admissão que envolvem departamento, cargo e disponibilidade do e-mail, consultando uma porta do domínio.
- **Na aplicação:** coordenar validação, criação/persistência do colaborador e registro do evento na Outbox.
- **No banco:** impedir e-mails duplicados e referências inválidas mesmo com requisições simultâneas.

Por exemplo, consultar um e-mail antes de gravar não resolve a concorrência: duas requisições podem consultar ao mesmo tempo e ambas encontrarem o e-mail disponível. A restrição de unicidade decide a gravação, e a API deve converter a violação esperada em erro de negócio.

### Objetos de valor e serviços de domínio

CostCenterCode e Email são objetos de valor. Email remove espaços nas extremidades e converte para minúsculas com `Locale.ROOT`, para a comparação aprovada pelo candidato. O formato implementado é uma política simples com limite técnico de 254 caracteres; não cobre todos os endereços possíveis nas RFCs nem comprova que o endereço exista. A unicidade entre colaboradores é protegida por constraint no banco; outra constraint exige que o valor gravado esteja em minúsculas e sem espaços externos.

O responsável técnico solicitou extrair as regras existentes de admissão para EmployeeRegistrationPolicy. O serviço decide se departamento e cargo existem e se o e-mail está disponível; essas informações envolvem outros registros e não pertencem somente à entidade Employee. EmployeeRegistrationRequirements é a porta de consulta do domínio; JpaEmployeeRegistrationRequirements fornece os fatos usando os repositórios JPA existentes.

A política e EmployeeRegistrationException não dependem de Spring, HTTP ou JPA. A configuração Spring instancia o serviço; CreateEmployee delega a validação e mantém a coordenação da persistência/evento. As regras e sua ordem continuam as mesmas, e as constraints continuam indispensáveis sob concorrência. A decisão 008 registra a alternativa de manter os checks na aplicação e o custo das classes adicionais.

## Propostas que precisam de revisão

| Tema | Proposta inicial | Questão ainda aberta |
|---|---|---|
| DELETE | API e evento integrados pelos PRs #3 e #5; retorna 204 para alteração ou repetição e 404 para id inexistente. | Acompanhar limites de publicação/consumo documentados nas decisões 006 e 007. |
| Inativação repetida | O domínio retorna false quando não há mudança; a API responde 204 sem nova gravação nem evento adicional. | Reenvios do mesmo evento lógico podem ocorrer e são deduplicados no histórico. |
| Listagem | Implementada com filtro opcional por nome e status, ativos e inativos por padrão e paginação. | Avaliar desempenho e estabilidade entre requisições conforme o volume de dados. |
| Busca por departamento | Implementada com nome exato sem distinguir caixa, paginação, todos os departamentos homônimos e lista vazia quando não há correspondência. | Avaliar requisitos de acentuação e desempenho conforme o uso. |

O limite de 120 caracteres para o nome de Employee foi aprovado pelo candidato. Limites de nome de departamento, centro de custo, título e nível são decisões técnicas iniciais, ainda não confirmadas como regras de negócio. Nível do cargo está como texto, sem lista fechada, e o código não exige unicidade de nome de departamento ou centro de custo.

## Transações e eventos

O candidato aprovou Kafka, Outbox e JPA para toda a persistência da aplicação. Cadastro e inativação registram seus eventos pela porta EmployeeEventOutbox, na mesma transação da mudança do empregado. O publicador envia após o commit; os consumidores gravam histórico idempotente por eventId. O responsável técnico escolheu dois tópicos preservando employee-created.v1 e acrescentando employee-status-changed.v1. As decisões 006 e 007 detalham classes, contratos, transações, testes e limites. Não há ordem garantida entre tópicos e o histórico não reaplica eventos ao agregado.

Kafka não torna a transação do banco e a publicação atômicas. Sem uma estratégia adicional, a aplicação pode confirmar o cadastro e falhar antes de publicar o evento. A Outbox permite registrar a intenção de publicação na mesma transação do colaborador, com mecanismo posterior de envio; também exige cuidados com duplicatas e operação. Não prometer exactly-once.

## Situação do trabalho de DDD

1. Problema e requisitos: lidos e identificados.
2. Linguagem compartilhada: iniciada, com definições confirmadas de inativo e cargo.
3. Regras e cenários: em discussão; consultar a tabela de propostas.
4. Contexto e agregados: propostos neste documento, ainda sujeitos à revisão.
5. Modelo: seis endpoints, ambos os eventos e serviço de domínio de admissão integrados à main. A paginação própria da aplicação e a igualdade por ID de Department/Position estão implementadas nesta branch para revisão, conforme decisão 009.
6. Validação do modelo com testes: domínio, aplicação, integração HTTP/banco, concorrência e ambos os eventos. Avaliação da cobertura permanece pendente.

Para explicar este passo: quais regras estão confirmadas, quais são propostas e por que uma regra pode precisar da entidade, do caso de uso e do banco trabalhando juntos?
