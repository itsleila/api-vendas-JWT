# API de Vendas com Autenticação JWT

Projeto desenvolvido com arquitetura de microsserviços para cadastro de clientes, produtos e realização de vendas. A aplicação utiliza um serviço separado de autenticação, descoberta de serviços com Eureka e um API Gateway como ponto de entrada.

## Arquitetura

Serviços disponíveis:

| Serviço | Porta | Responsabilidade |
|---|---:|---|
| `eureka-server` | `8761` | Registro e descoberta dos microsserviços |
| `config-server` | `8888` | Centralização das configurações |
| `produtos-service` | `8081` | Gerenciamento de produtos |
| `vendas-service` | `8082` | Gerenciamento de pedidos e vendas |
| `clientes-service` | `8083` | Gerenciamento de clientes |
| `auth-service` | `8084` | Cadastro de usuários, login e renovação de tokens |
| `gateway` | `8085` | Ponto de entrada, roteamento e validação do JWT |

Fluxo:

```text
Cliente/Postman
      |
      v
API Gateway (8085) ----> valida o access token
      |
      +----> Auth Service
      +----> Clientes Service
      +----> Vendas Service
      +----> Produtos Service
                  |
                  v
            Eureka Server
```

Os serviços são executados em containers Docker e se comunicam pela rede interna definida no `docker-compose.yml`. O Gateway localiza os microsserviços pelo Eureka utilizando endereços no formato `lb://NOME-DO-SERVICO`.

## Tecnologia de autenticação

Foi escolhido o **JWT (JSON Web Token)**, implementado com Spring Security.

O `auth-service` verifica o e-mail e a senha do usuário e gera dois tokens:

- **Access token:** utilizado para acessar as rotas protegidas. Possui duração menor.
- **Refresh token:** utilizado exclusivamente para obter um novo par de tokens sem realizar outro login.

As senhas são armazenadas no banco H2 utilizando hash BCrypt. O Gateway valida a assinatura, a expiração e o tipo do token antes de encaminhar uma requisição protegida.

Para fins acadêmicos, o `auth-service` e o Gateway compartilham a mesma chave secreta por variável de ambiente. Em uma aplicação de produção, seria recomendável utilizar chaves assimétricas ou uma solução dedicada de identidade, como Keycloak.

## Como executar

### Pré-requisitos

- Docker;
- Docker Compose.

Na mesma pasta do `docker-compose.yml`, crie um arquivo `.env` adcionando uma JWT_SECRET

Para construir as imagens e iniciar os serviços:

```bash
docker compose up -d --build
```

Para verificar os containers:

```bash
docker compose ps
```

Todos os testes externos devem ser realizados pelo Gateway:

```text
http://localhost:8085
```

## Endpoints públicos

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/usuarios/cadastro` | Cadastra um usuário |
| `POST` | `/api/auth/login` | Autentica o usuário e retorna os tokens |
| `POST` | `/api/auth/refresh` | Renova o access token utilizando o refresh token |

## Endpoints protegidos

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/clientes` | Lista os clientes |
| `POST` | `/api/pedidos` | Cria um pedido |
| `GET` | `/api/containers` | Lista os containers |

Uma requisição sem token, com token inválido ou expirado deve retornar `401 Unauthorized`.

## Cadastro de usuário

```http
POST http://localhost:8085/api/usuarios/cadastro
Content-Type: application/json
```

```json
{
  "nome": "Usuário Teste",
  "email": "teste@email.com",
  "senha": "123456"
}
```

Resposta esperada: `201 Created`.

```json
{
  "id": 1,
  "nome": "Usuário Teste",
  "email": "teste@email.com",
  "ativo": true
}
```

## Como realizar a autenticação

```http
POST http://localhost:8085/api/auth/login
Content-Type: application/json
```

```json
{
  "email": "teste@email.com",
  "senha": "123456"
}
```

Resposta esperada: `200 OK`.

```json
{
  "tokenType": "Bearer",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 900000
}
```

O `accessToken` deve ser enviado nas rotas protegidas. O `refreshToken` deve ser utilizado somente no endpoint de renovação.

## Como utilizar o refresh token

```http
POST http://localhost:8085/api/auth/refresh
Content-Type: application/json
```

```json
{
  "refreshToken": "SEU_REFRESH_TOKEN"
}
```

Resposta esperada: `200 OK`, com um novo par de tokens:

```json
{
  "tokenType": "Bearer",
  "accessToken": "NOVO_ACCESS_TOKEN",
  "refreshToken": "NOVO_REFRESH_TOKEN",
  "expiresIn": 900000
}
```

Após a renovação, a aplicação cliente deve substituir os tokens anteriores pelos novos.

## Exemplos de testes

### Acesso sem autenticação

```http
GET http://localhost:8085/api/clientes
```

Resposta esperada:

```text
401 Unauthorized
```

### Acesso com access token

```http
GET http://localhost:8085/api/clientes
Authorization: Bearer SEU_ACCESS_TOKEN
```

Resposta esperada:

```text
200 OK
```

### Criação de pedido autenticada

O corpo deve ser adaptado ao DTO do `vendas-service`:

```http
POST http://localhost:8085/api/pedidos
Authorization: Bearer SEU_ACCESS_TOKEN
Content-Type: application/json
```

```json
{
  "clienteId": 1,
  "produtoId": 1,
  "quantidade": 2
}
```

Resposta esperada: `200 OK` ou `201 Created`, conforme a implementação do controller.

### Credenciais inválidas

```http
POST http://localhost:8085/api/auth/login
Content-Type: application/json
```

```json
{
  "email": "teste@email.com",
  "senha": "senha-incorreta"
}
```

Resposta esperada:

```text
401 Unauthorized
```

### Refresh token usado como access token

```http
GET http://localhost:8085/api/clientes
Authorization: Bearer SEU_REFRESH_TOKEN
```

Resposta esperada:

```text
401 Unauthorized
```

### Access token enviado no endpoint de refresh

```http
POST http://localhost:8085/api/auth/refresh
Content-Type: application/json
```

```json
{
  "refreshToken": "SEU_ACCESS_TOKEN"
}
```

Resposta esperada:

```text
401 Unauthorized
```
- Um retorno `503 Service Unavailable` normalmente indica que o Gateway não encontrou o serviço no Eureka.
- Um retorno `404 Not Found` normalmente indica que o caminho encaminhado pelo Gateway não corresponde ao mapeamento do controller.
