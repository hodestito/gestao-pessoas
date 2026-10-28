# Executar a API com Postman

Importe estes dois arquivos no Postman Desktop:

- [Collection](gestao-pessoas.postman_collection.json), formato Postman v2.1.
- [Environment local](local.postman_environment.json), com baseUrl=http://localhost:8080.

A aplicação precisa estar iniciada. Para execução completa com PostgreSQL/Kafka, use `docker compose up -d --build --wait` na raiz do repositório. Para H2, use `./mvnw spring-boot:run`. Os testes desta collection validam HTTP; eles não confirmam publicação/consumo Kafka ou quantidade de eventos na Outbox. Para isso, consulte o [roteiro e sua consulta SQL](../docs/presentation.md).

## Executar o fluxo

1. Selecione o environment **Gestão de Pessoas — local**.
2. Ajuste baseUrl se necessário, sem barra no final. Na demonstração com porta 18080, use http://localhost:18080.
3. Abra a collection e execute **Run collection**, em modo local, mantendo a ordem e começando com uma iteração.
4. Confira os resultados dos testes. Os requests negativos esperam 400, 404, 409 ou 422 e devem passar nas asserções.

O request 00 verifica /actuator/health, gera nomes/e-mail para a rodada e limpa os IDs anteriores. Os requests 01–03 capturam departmentId, positionId e employeeId nos corpos de resposta, salvando-os como variáveis da collection. As chamadas seguintes usam esses IDs automaticamente.

O environment declara apenas baseUrl. Não acrescente IDs vazios nele: variáveis de environment têm precedência sobre as de collection e poderiam ocultar os IDs capturados. Ao executar chamadas isoladas, faça primeiro 00, 01, 02 e 03. O request 00 inicia uma nova rodada e troca as referências utilizadas depois.

O Runner pode manter alterações de variáveis apenas durante a execução; use a opção de preservar valores quando quiser reutilizá-los em chamadas manuais após o run. A collection inteira não depende dessa persistência entre runs, pois cada rodada prepara os próprios dados.

## Requests e resultado esperado

| Etapa | Requisição | Resultado |
|---|---|---|
| 00 | GET /actuator/health | 200, UP; prepara variáveis |
| 01–02 | POST /departments e POST /positions | 201, Location e IDs capturados |
| 03 | POST /employees | 201, ACTIVE, e-mail normalizado e ID capturado |
| 04–05 | GET /employees e GET /employees/search | 200, cadastro localizado na página |
| 06 | Repetir e-mail | 409, EMAIL_ALREADY_EXISTS |
| 07–08 | Referências inexistentes | 422, DEPARTMENT_NOT_FOUND/POSITION_NOT_FOUND |
| 09 | Página negativa | 400, INVALID_INPUT |
| 10 | DELETE de UUID inexistente | 404 sem corpo |
| 11–12 | DELETE do colaborador duas vezes | 204 sem corpo nas duas |
| 13 | GET /employees com INACTIVE | 200, cadastro preservado e inativo |
| 14 | Mesmo e-mail após inativação | 409, EMAIL_ALREADY_EXISTS |
| 15 | Busca por departamento após inativação | 200, inclui o inativo |

Cada rodada cria um departamento, um cargo e um colaborador que termina INACTIVE. Os dados são preservados; não existe exclusão física por essa collection. Nomes/e-mail usam um UUID novo, permitindo repetir sem limpeza manual. Os UUIDs de referências inexistentes também são gerados na rodada.

As asserções verificam status, erros esperados, IDs/Location, campos da página, normalização e estado. Retornar 204 duas vezes, isoladamente, não prova ausência de evento duplicado; essa garantia também é exercitada pelos testes Java e pela consulta SQL do roteiro.

## Newman opcional

Se Node.js/npm estiverem disponíveis, execute da raiz do repositório:

```sh
npx --yes newman@6.2.3 run postman/gestao-pessoas.postman_collection.json \
  -e postman/local.postman_environment.json
```

Para outro endereço e duas rodadas:

```sh
npx --yes newman@6.2.3 run postman/gestao-pessoas.postman_collection.json \
  -e postman/local.postman_environment.json \
  --env-var baseUrl=http://localhost:18080 --iteration-count 2
```

Node/npm não são necessários para importar/executar pelo Postman Desktop. Os comandos Newman não são parte do workflow de CI atual.

## Validação realizada

Collection validada estruturalmente contra o schema oficial Postman v2.1.0. Newman 6.2.3 executou duas rodadas contra a aplicação em ambiente Compose isolado: 32 requests e 84 asserções, zero falhas. Cada rodada gerou dados/IDs novos e preservou o estado esperado na sequência. Os scripts usam escopo próprio para evitar conflito de variáveis no sandbox. Isso não constitui uma verificação visual da interface Postman Desktop.

Referências: [variáveis e precedência](https://learning.postman.com/docs/tests-and-scripts/write-scripts/postman-sandbox-reference/pm-variables), [Collection Runner](https://learning.postman.com/docs/tests-and-scripts/running-collections/intro-to-collection-runs), [Newman](https://learning.postman.com/docs/collections/using-newman-cli/installing-running-newman).
