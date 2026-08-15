# 🛒 Monitoramento Inteligente de Preços (API)

Aplicação backend focada no rastreamento automatizado e inteligente de preços no varejo online (com foco na Kabum). O sistema permite que os usuários cadastrem "Missões de Busca" para produtos específicos. Um motor de Web Scraping varre as vitrines periodicamente e notifica o usuário via WhatsApp em caso de descontos agressivos ou recordes de preço baixo.

## 🚀 Status do Projeto
**Fase Atual:** Desenvolvimento do Domínio Core e Infraestrutura.

Funcionalidades já implementadas:
* [x] Setup do esqueleto Spring Boot e CI/CD (GitHub Actions) com bancos de dados efêmeros.
* [x] **Domínio de Usuário:** Cadastro com validação estrita e tratamento global de exceções (RFC 7807).
* [x] **Segurança Criptográfica:** Hashing de senhas de altíssima segurança com `Argon2id` + `Pepper`.
* [x] **Autenticação e Autorização:** Proteção de rotas com Spring Security, validação de JSON Web Tokens (JWT) e extração de identidade segura via `JwtAuthenticationToken`.
* [x] **Domínio de Monitoramento (NoSQL):** Persistência de dados poliglota. Cadastro de "Missões de Busca" vinculadas ao usuário de forma segura, com bloqueio contra buscas duplicadas.
* [x] **Inteligência de Filtragem:** Extração e fatiamento automático de palavras-chave a partir da intenção do usuário e suporte a *Blacklists* (palavras proibidas) para blindar a precisão futura do robô de Web Scraping.

## 🛠️ Stack Tecnológico
* **Linguagem:** Java 21
* **Framework:** Spring Boot 3.4.x
* **Build Tool:** Gradle
* **Segurança:** Spring Security, JWT (JSON Web Tokens), BouncyCastle (Argon2id)
* **Bancos de Dados:** PostgreSQL (Relacional) e MongoDB (NoSQL)
* **Integrações Futuras:** Jsoup (Scraping), WhatsApp API

## 🏛️ Destaques Arquiteturais

### 1. Monolito Poliglota
O sistema foi desenhado como um monolito modular que utiliza o melhor de dois mundos na persistência de dados:
* **PostgreSQL:** Gerencia dados altamente estruturados e relacionais (Ex: Usuários, Credenciais e Permissões).
* **MongoDB:** Responsável pelo armazenamento de alto volume e esquema dinâmico (Ex: Missões de Busca e a Série Histórica temporal de preços diários).

### 2. Desenvolvimento Orientado a "Vertical Slice"
Toda a produção do código segue estritamente a orientação de *Vertical Slice* (Fatia Vertical). Cada funcionalidade (ex: Cadastro de Usuário ou Login) é desenvolvida de ponta a ponta na mesma branch — cruzando a camada de Infraestrutura, Domínio e API de forma atômica e coesa — garantindo que nenhuma feature incompleta chegue à branch principal.

### 3. Segurança Anti-Mass Assignment
A aplicação é blindada contra ataques de Injeção de Propriedades (*Mass Assignment*). Nenhuma Entidade pode vazar pela API em nenhuma circunstância. O tráfego de dados é feito estritamente através de `DTOs` construídos através da Regra do Builder Obrigatório. Além disso, dados sensíveis como o ID do usuário e seu nível de acesso (`Role`) são extraídos cirurgicamente do token JWT pela arquitetura e nunca através de *payloads* manipuláveis vindos da requisição.

## ⚙️ Como Executar o Projeto Localmente
*(Em construção: Instruções de infraestrutura Docker e variáveis de ambiente serão adicionadas em breve).*