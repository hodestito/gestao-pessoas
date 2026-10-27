# 001 — Identidade de Employee e direção para eventos

Estado: aprovado pelo responsável técnico para o modelo inicial; estratégia de publicação ainda pendente. Data: 27/10/2026. O cadastro posterior está registrado na [decisão 002](002-cadastro-employee.md).

## Contexto e decisões

- Employee é uma classe de domínio independente de frameworks, com identidade estável por UUID e igualdade/hashCode baseados somente no id. Alternativa: record com igualdade por todos os campos; a escolha de classe facilita expressar transição de estado sem mudar a identidade.
- O objeto de valor Email normaliza espaços nas extremidades e caixa usando `Locale.ROOT`; e-mails são únicos inclusive para inativos. A validação de formato é uma política prática limitada, não validação completa das RFCs. A garantia entre colaboradores foi posteriormente implementada conforme a decisão 002.
- Departamento e cargo devem existir; nome tem até 120 caracteres. O agregado valida que seus identificadores estão presentes; a verificação de existência e sua proteção no banco foram posteriormente implementadas conforme a decisão 002.
- A criação recebe id e Instant e inicia ACTIVE. Receber o instante permite testes determinísticos sem acoplar o domínio ao relógio de sistema. Uma segunda inativação não altera o estado e retorna false; o contrato HTTP e a publicação do evento ainda serão decididos.
- O responsável técnico selecionou Kafka para eventos, em linha com a preferência do desafio. A alternativa Spring Events é mais simples, mas não oferece os mesmos recursos de broker. Nenhuma mensageria foi implementada; a decisão sobre Outbox permanece para o incremento de eventos.

## Consequências e verificação

As conversões entre domínio e persistência e a reconstituição foram implementadas no cadastro posterior. O objeto de domínio não consulta bancos nem valida unicidade por conta própria. Kafka sem Outbox não garante atomicidade entre banco e publicação.

Testes unitários cobrem criação, normalização, campos obrigatórios, igualdade por id e inativação repetida. A verificação de referências, a concorrência de e-mail e a mensageria exigirão testes adicionais em seus respectivos incrementos.
