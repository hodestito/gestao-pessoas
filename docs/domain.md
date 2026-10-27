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
| E-mail de colaborador deve ser único. | Candidato | Caso de uso pode antecipar o erro; restrição no banco garante a regra sob concorrência. |
| Inativação representa saída da empresa. | Candidato | Comportamento do agregado Employee. |
| Criar colaborador produz EmployeeCreatedEvent. | Enunciado | Fluxo de cadastro e publicação de eventos. |
| Inativar colaborador produz EmployeeStatusChangedEvent. | Enunciado | Mudança de estado e publicação de eventos. |
| Consumidores geram logs e atualizam uma tabela de histórico. | Enunciado | Consumidores de eventos e persistência do histórico. |

As garantias acima para colaboradores ainda não estão implementadas.

## Agregados e invariantes: proposta de modelagem

Uma **invariante** é uma regra que precisa continuar verdadeira após uma operação válida. Um **agregado** define um limite de consistência e tem uma raiz pela qual suas mudanças são realizadas. Não é necessário agrupar toda a estrutura organizacional em um objeto.

| Raiz proposta | Responsabilidade | Referências |
|---|---|---|
| Employee | Proteger os dados e as mudanças de estado do colaborador. | departmentId e positionId. |
| Department | Proteger os dados do departamento. | Não contém uma coleção de todos os colaboradores. |
| Position | Proteger os dados do cargo. | Não contém uma coleção de todos os colaboradores. |

Employee ainda será modelado. Department e Position já existem como registros imutáveis com validações de construção; isso, isoladamente, não demonstra todo o DDD exigido pelo desafio.

### Quem protege cada tipo de regra

- **Dentro de Employee:** validade dos seus valores e transições permitidas de estado.
- **Na aplicação:** coordenar a consulta a departamento/cargo e a gravação do colaborador.
- **No banco:** impedir e-mails duplicados e referências inválidas mesmo com requisições simultâneas.

Por exemplo, consultar um e-mail antes de gravar não resolve a concorrência: duas requisições podem consultar ao mesmo tempo e ambas encontrarem o e-mail disponível. A restrição de unicidade decide a gravação, e a API deve converter a violação esperada em erro de negócio.

### Objetos de valor e serviços de domínio

CostCenterCode já existe como objeto de valor. Email é uma proposta para concentrar validação e uma política explícita de comparação. Precisamos definir essa política antes de implementá-la.

O enunciado exige serviços de domínio. Ainda precisamos identificar e justificar qual regra de negócio, independente de frameworks, deve ser expressa por esse serviço. Os casos de uso existentes são serviços de aplicação; não devem ser apresentados como serviços de domínio apenas para cumprir a nomenclatura.

## Propostas que precisam de revisão

| Tema | Proposta inicial | Questão ainda aberta |
|---|---|---|
| E-mail de inativos | Manter unicidade entre ativos e inativos. | Como tratar recontratação ou reutilização do endereço? |
| Comparação do e-mail | Definir normalização antes de persistir. | Quais diferenças de caixa ou espaços representam o mesmo endereço? |
| Cargo existente | Exigir Position existente para cadastrar Employee. | Confirmar como regra de negócio. |
| Estado inicial | Criar colaborador como ACTIVE. | Confirmar se o cadastro sempre representa pessoa já ativa. |
| DELETE | Inativar e preservar o registro. | Confirmar resposta e tratamento da repetição. |
| Inativação repetida | Retornar sucesso sem gerar novo evento. | Confirmar contrato idempotente. |
| Listagem | Incluir ativos/inativos e paginar. | Confirmar tamanho, ordenação e limites. |
| Busca | Nome exato do departamento, sem distinguir caixa, com paginação. | Definir comportamento para nomes repetidos e departamento inexistente. |

Limites de tamanho de nome, centro de custo, título e nível presentes no código são decisões técnicas iniciais; ainda não são regras de negócio confirmadas. Nível do cargo está como texto, sem lista fechada, e o código não exige unicidade de nome de departamento ou centro de custo.

## Transações e eventos

O candidato indicou interesse em Outbox e pediu para evitar aumentar a complexidade neste momento. A proposta apresentada foi começar com Spring Events e consumo assíncrono após o commit. Essa estratégia ainda precisa de revisão antes da implementação.

Nesse desenho, a gravação do colaborador ocorre na transação principal. Somente após o commit, o consumidor processa o evento e grava o histórico em outra transação. Uma falha do consumidor não desfaz o cadastro já confirmado.

**Limitação:** pode haver perda do evento entre o commit e o processamento, inclusive numa queda da aplicação. O histórico pode ficar incompleto. Retentativas em memória não oferecem a durabilidade de uma Outbox. Não prometer entrega garantida nem exactly-once.

A Outbox seria uma evolução para gravar a intenção de publicação na mesma transação do colaborador e permitir retomada. Publicação via interface ajuda a reduzir acoplamento, mas não cria essa garantia automaticamente.

## Situação do trabalho de DDD

1. Problema e requisitos: lidos e identificados.
2. Linguagem compartilhada: iniciada, com definições confirmadas de inativo e cargo.
3. Regras e cenários: em discussão; consultar a tabela de propostas.
4. Contexto e agregados: propostos neste documento, ainda sujeitos à revisão.
5. Modelo de colaboradores e limites transacionais: ainda não implementados.
6. Validação do modelo com testes: iniciada apenas para departamentos e cargos.

Para explicar este passo: quais regras estão confirmadas, quais são propostas e por que uma regra pode precisar da entidade, do caso de uso e do banco trabalhando juntos?
