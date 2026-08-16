# 🛒 Monitoramento Inteligente de Preços (API)

Aplicação backend focada no rastreamento automatizado e inteligente de preços no varejo online (com foco na Kabum). O sistema não depende de URLs estáticas; os usuários cadastram termos de busca precisos, e o sistema varre a vitrine da loja a cada 12 horas. Ele analisa os 5 resultados mais relevantes para criar uma média de mercado e rastrear a melhor oferta, notificando o usuário via WhatsApp apenas quando identificar descontos agressivos ou quando o preço cair abaixo do limite estipulado pelo usuário.

**Motivação Arquitetural:** A decisão de não fixar URLs garante resiliência ao sistema. Se a loja mudar a sua árvore de categorias ou a estrutura dos links internos, o monitoramento se mantém intacto, pois a aplicação simula uma pesquisa humana orgânica diretamente no motor de busca do e-commerce.

## 🚀 Status do Projeto
**Fase Atual:** Desenvolvimento do Domínio Core e Infraestrutura.

Funcionalidades já implementadas:
* [x] Setup do esqueleto Spring Boot e CI/CD (GitHub Actions) com bancos de dados efêmeros.
* [x] **Domínio de Usuário:** Cadastro com validação estrita e tratamento global de exceções (RFC 7807).
* [x] **Segurança Criptográfica:** Hashing de senhas de altíssima segurança com `Argon2id` + `Pepper`.
* [x] **Autenticação e Autorização:** Proteção de rotas com Spring Security, validação de JSON Web Tokens (JWT) e extração de identidade segura via `JwtAuthenticationToken`.
* [x] **Domínio de Monitoramento (NoSQL):** Persistência de dados poliglota. Cadastro de "Missões de Busca" vinculadas ao usuário de forma segura, com bloqueio contra buscas duplicadas.
* [x] **Inteligência de Filtragem:** Extração e fatiamento automático de palavras-chave a partir da intenção do usuário e suporte a *Blacklists* (palavras proibidas) para blindar a precisão futura do robô de Web Scraping.
    * **Por que isso é necessário?** Mecanismos de busca de e-commerces frequentemente retornam produtos acessórios (ex: buscar "RTX 4060" retorna "Cabo de força para RTX 4060"). O filtro de similaridade previne que o preço baixo desses acessórios contamine e destrua a confiabilidade da nossa Média de Mercado.
* [x] **Motor de Web Scraping:** Integração com Jsoup para extração dinâmica do HTML da vitrine de resultados. O algoritmo burla bloqueios injetando *User-Agents* reais e coleta o preço e a URL exata estritamente dos 5 melhores resultados que sobreviverem ao funil de filtragem.

## 🛠️ Stack Tecnológico
* **Linguagem:** Java 21
* **Framework:** Spring Boot 3.4.x
* **Build Tool:** Gradle
* **Segurança:** Spring Security, JWT (JSON Web Tokens), BouncyCastle (Argon2id)
* **Bancos de Dados:** PostgreSQL (Relacional) e MongoDB (NoSQL)
* **Integrações Atuais:** Jsoup (Web Scraping HTML)
* **Integrações Futuras:** API do WhatsApp (Mensageria)

## 🏛️ Destaques Arquiteturais

### 1. Monolito Poliglota
O sistema foi desenhado como um monolito modular que utiliza o melhor de dois mundos na persistência de dados:
* **PostgreSQL:** Gerencia dados altamente estruturados e com necessidade de integridade referencial ACID (Ex: Usuários, Credenciais e Permissões).
* **MongoDB:** Responsável pelo armazenamento de alto volume e esquema dinâmico (Ex: Missões de Busca e a Série Histórica temporal de preços diários).
* **Motivação:** O histórico de preços de uma missão sofre anexações diárias. Salvar uma série histórica crescente em bancos relacionais exigiria queries complexas e tabelas massivas. O MongoDB lida com sub-documentos aninhados de forma nativa, permitindo extrair e anexar a variação de preços em milissegundos.

### 2. Desenvolvimento Orientado a "Vertical Slice"
Toda a produção do código segue estritamente a orientação de *Vertical Slice* (Fatia Vertical). Cada funcionalidade (ex: Cadastro de Usuário ou Login) é desenvolvida de ponta a ponta na mesma branch — cruzando a camada de Infraestrutura, Domínio e API de forma atômica e coesa.
* **Motivação:** Essa prática garante que nenhuma funcionalidade chegue incompleta à branch principal, forçando a resolução do ciclo completo da feature antes do merge. Isso elimina o acúmulo de código "Frankenstein" e rotas ociosas sem regras de domínio estabelecidas.

### 3. Segurança Anti-Mass Assignment e Blindagem de API
A aplicação é protegida contra ataques de Injeção de Propriedades (*Mass Assignment*). Nenhuma Entidade vaza pela API, sendo o tráfego de dados isolado por `DTOs` construídos através do padrão Builder.
* **Motivação da Extração Segura:** Dados sensíveis como o ID do usuário, data de criação e seu nível de acesso (`Role`) nunca são confiados ao *payload* da requisição. A arquitetura exige que o `JwtAuthenticationToken` seja resolvido pelo Spring Security e injetado diretamente na assinatura dos métodos. Isso mitiga totalmente tentativas de escalonamento de privilégios ou fraudes de ID vindas do frontend.

## ⚙️ Como Executar o Projeto Localmente
*(Em construção: Instruções de infraestrutura Docker e variáveis de ambiente serão adicionadas em breve).*