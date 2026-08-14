# 🛒 Monitoramento Inteligente de Preços (API)

Aplicação backend focada no rastreamento automatizado e inteligente de preços no varejo online (com foco em hardwares e eletrônicos). O sistema permite que os usuários cadastrem "Missões de Busca" para produtos específicos. Um motor de Web Scraping varre as vitrines periodicamente e notifica o usuário via WhatsApp em caso de descontos agressivos ou recordes de preço baixo.

## 🚀 Status do Projeto
**Fase Atual:** Desenvolvimento do Domínio Core e Infraestrutura.

Funcionalidades já implementadas:
* [x] Setup do esqueleto Spring Boot e CI/CD (GitHub Actions).
* [x] **Domínio de Usuário:** Cadastro com validação estrita e tratamento global de exceções (RFC 7807).
* [x] **Segurança Criptográfica:** Hashing de senhas com `Argon2id` + `Pepper`.

## 🛠️ Stack Tecnológico
* **Linguagem:** Java 21
* **Framework:** Spring Boot 3.4.x
* **Build Tool:** Gradle
* **Segurança:** Spring Security, JWT (JSON Web Tokens), BouncyCastle (Argon2)
* **Bancos de Dados:** PostgreSQL (Relacional) e MongoDB (NoSQL)
* **Integrações Futuras:** Jsoup (Scraping), WhatsApp API

## 🏛️ Destaques Arquiteturais

### 1. Monolito Poliglota
O sistema foi desenhado como um monolito modular que utiliza o melhor de dois mundos na persistência de dados:
* **PostgreSQL:** Gerencia dados altamente estruturados e relacionais (Ex: Usuários, Credenciais e Permissões).
* **MongoDB:** Responsável pelo armazenamento de alto volume e esquema dinâmico (Ex: Missões de Busca e a Série Histórica temporal de preços diários).

### 2. Desenvolvimento Orientado a "Vertical Slice"
A arquitetura de entrega não é baseada em camadas soltas, mas em Fatias Verticais (*Vertical Slices*). Cada nova funcionalidade (ex: Cadastro de Usuário) é desenvolvida de ponta a ponta na mesma branch — desde a modelagem no banco de dados (`Entity`), passando pelas regras de negócio (`Service`), até a exposição limpa na API (`Controller`) e o tratamento de caminhos de erro (`409 Conflict`) — garantindo que nenhuma feature incompleta chegue à branch principal.

### 3. Segurança Anti-Mass Assignment
A aplicação é blindada contra ataques de Injeção de Propriedades (*Mass Assignment*). As Entidades do banco de dados nunca são expostas em endpoints. O tráfego de dados é feito estritamente através de `DTOs` imutáveis (construídos via *Builder Pattern*). Além disso, o nível de acesso (`Role`) é controlado exclusivamente pelo backend e embutido no payload do JWT, impedindo que requisições maliciosas tentem forjar privilégios de administrador.

## ⚙️ Como Executar o Projeto Localmente
*(Em construção: Instruções de infraestrutura Docker e variáveis de ambiente serão adicionadas em breve).*