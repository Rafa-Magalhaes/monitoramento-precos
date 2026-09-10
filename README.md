# 🛒 Monitoramento Inteligente de Preços (API)

Aplicação backend focada no rastreamento automatizado e inteligente de preços no varejo online (com foco no Mercado Livre). O sistema não depende de URLs estáticas; os usuários cadastram termos de busca precisos, e o sistema varre a vitrine da loja a cada 12 horas. Ele analisa os 5 resultados mais relevantes para criar uma média de mercado e rastrear a melhor oferta, notificando o usuário via WhatsApp apenas quando identificar descontos agressivos ou quando o preço cair abaixo do limite estipulado pelo usuário.

**Motivação Arquitetural:** A decisão de não fixar URLs garante resiliência ao sistema. Se a loja mudar a sua árvore de categorias ou a estrutura dos links internos, o monitoramento se mantém intacto, pois a aplicação simula uma pesquisa humana orgânica diretamente no motor de busca do e-commerce.

---

## 🏛️ Arquitetura do Sistema

<p align="center">
  <img src="organograma.png" alt="Diagrama de Arquitetura do Sistema" width="100%">
</p>

> 💡 **Legenda Arquitetural do Sistema:**
> - **Camada de Entrada (Cinza):** Gatilhos temporais (Cron Jobs) rodando em UTC e ações do usuário.
> - **Camada de API (Azul claro):** Entrypoints protegidos por Spring Security, Rate Limiting (Bucket4j) e filtro JWT.
> - **Camada de Domínio (Lilás/Amarelo):** O "Cérebro" do robô (`MonitoramentoWorker`), gerindo filas de retry, regras de negócio e ownership.
> - **Camada de Infraestrutura (Vermelho):** Conectores de banco e o motor de Web Scraping (Bright Data + Jsoup).
> - **Base e Destinos (Amarelo/Verde):** Persistência poliglota (PostgreSQL Supabase + MongoDB Atlas) e notificações oficiais via Meta Cloud API (WhatsApp).

---

## 🚀 Status do Projeto
**Fase Atual:** Sistema operacional em Produção (GCP). Domínio Core, Infraestrutura Cloud, Mensageria Oficial e Observabilidade finalizados.

### Funcionalidades já implementadas:

- [x] **Setup Cloud-Native e CI/CD:** Esteira no GitHub Actions gerando build automatizado e enviando a imagem otimizada para o Docker Hub.
- [x] **Domínio de Usuário (Gestão de Identidade):** Cadastro com validação estrita, higienização de dados na borda da API (Case-Insensitive e Trim) e tratamento global de exceções.
- [x] **Segurança Criptográfica e Rate Limiting:** Hashing de senhas de altíssima segurança com Argon2id + Pepper. Proteção ativa contra Força Bruta e DDoS de camada 7 utilizando algoritmos de Token Bucket (Bucket4j).
- [x] **Autenticação e Autorização:** Proteção de rotas com Spring Security, validação de JSON Web Tokens (JWT contendo ID e Role) e extração de identidade segura via `JwtAuthenticationToken`.
- [x] **Domínio de Monitoramento (NoSQL):** Persistência poliglota. CRUD completo de "Missões de Busca" vinculadas ao usuário de forma segura. A edição de termos engatilha o recálculo automático da inteligência do robô, com Validação de Propriedade (Ownership) estrita.
- [x] **Inteligência de Filtragem (Stop Words):** Extração de palavras-chave eliminando preposições inúteis (Stop Words) para manter a precisão de termos curtos (ex: "S24", "M3"). O bloqueio de duplicidade é matemático, baseado em uma "Assinatura de Busca" ordenada, ignorando a ordem da digitação.
- [x] **Sanitização de Blacklists:** Tratamento automático de arrays de palavras proibidas, bloqueando `NullPointerExceptions` e limpando espaços ocos na entrada da API.
- [x] **Motor de Web Scraping Anti-Bot:** Integração com Jsoup e túnel de IP via **Bright Data**. O algoritmo burla WAFs pesados extraindo metadados JSON-LD com um Fallback nativo para varredura de HTML em caso de mudança de DOM.
- [x] **Motor Autônomo (Scheduler) e Resiliência:** Orquestra a raspagem a cada 12 horas exatas (09:00 e 21:00 BRT, processados em UTC). Possui três barreiras de resiliência:
    - *Fila de Retry em Memória:* Falhas de rede ou Timeouts severos sofrem reprocessamento tático (até **5 tentativas**) antes do cancelamento da extração diária.
    - *Alarme Crítico (Health Check):* Se a taxa de falha da rotina atingir **30%**, o sistema envia um diagnóstico de emergência ao WhatsApp do Admin.
    - *Coveiro de Missões Zumbis:* Limpeza diária (03:00 UTC) desativando buscas ativas há mais de 6 meses.
- [x] **Mensageria Oficial e Compliance (WhatsApp Cloud API):** Operando em ambiente de Produção oficial. Filtro geográfico inteligente para tratamento automático do 9º dígito em DDDs brasileiros (> 27) antes do envio para os servidores do Facebook.
- [x] **Observabilidade:** Exposição cirúrgica do `/actuator/health` para auditoria do PostgreSQL e MongoDB via UptimeRobot.

---

## 🛠️ Stack Tecnológico
*   **Linguagem:** Java 21
*   **Framework:** Spring Boot 3.4.x
*   **Segurança:** Spring Security, JWT, Argon2id + Pepper, Bucket4j (Rate Limiting)
*   **Bancos de Dados (DBaaS):** PostgreSQL (Supabase com RLS ativo) e MongoDB (Atlas)
*   **Integrações:** Jsoup, OpenFeign, Bright Data (Web Unlocker), WhatsApp Cloud API (Meta)
*   **Monitoramento e Testes:** Actuator, Swagger OpenAPI, JUnit 5, Mockito

---

## 🏛️ Destaques Arquiteturais

### 1. Monolito Poliglota (Cloud Distributed)
O sistema opera de forma 100% distribuída:
*   **PostgreSQL (Supabase):** Integridade relacional ACID para usuários. Protegido ativamente na nuvem com *Row-Level Security* (RLS).
*   **MongoDB (Atlas):** Armazenamento de alto volume com esquema dinâmico e Índices Compostos para garantir unicidade lógica em arrays de Histórico Diário de Preços.

### 2. Desenvolvimento Orientado a "Vertical Slice"
Cada funcionalidade é desenvolvida de ponta a ponta na mesma branch (Infraestrutura → Domínio → API), com exigência de cobertura estrita de Testes Unitários nas camadas de Serviço antes do merge.

### 3. Segurança Anti-Mass Assignment e Over-posting
Nenhuma Entidade vaza pela API, sendo o tráfego isolado por DTOs instanciados via Builder. Dados sensíveis (IDs, Roles) nunca são extraídos do payload JSON, mas sim do Token JWT resolvido em tempo de execução.

### 4. Orquestração Cross-Database (Exclusão em Cascata)
Sem Foreign Keys nativas entre SQL e NoSQL, a exclusão de uma conta exige orquestração em software. O fluxo expurga primeiramente os documentos no MongoDB e, apenas em caso de sucesso, comita a exclusão no Postgres via `@Transactional`.

---

## ☁️ Acesso ao Ambiente na Nuvem (Live)
O projeto está 100% operacional em nuvem, rodando em uma instância de 1GB do Google Cloud Platform (GCP). Toda a iteração de cadastro e gestão deve ser feita pelos endpoints oficiais:

🔗 **Documentação (Swagger UI):** `http://34.170.234.74/swagger-ui/index.html#/`

🩺 **Monitoramento (Actuator):** `http://34.170.234.74/actuator/health`

## ⚙️ Como Executar a Imagem Docker (Self-Hosting / Local)

1. **Crie o arquivo `.env` na raiz do diretório:**
   O sistema injeta credenciais dinamicamente no Docker. Preencha com as suas chaves:

```env
# Bancos de Dados
SPRING_DATASOURCE_URL=jdbc:postgresql://[URL_SUPABASE]:5432/postgres
SPRING_DATASOURCE_USERNAME=[USER]
SPRING_DATASOURCE_PASSWORD=[PASS]
SPRING_DATA_MONGODB_URI=mongodb+srv://[URI_ATLAS]

# Segurança e Criptografia
API_SECURITY_TOKEN_SECRET=[CHAVE_JWT]
API_SECURITY_PEPPER=[CHAVE_PEPPER_ARGON2]

# Integração Web Scraping (Bright Data)
API_BRIGHTDATA_HOST=brd.superproxy.io
API_BRIGHTDATA_PORT=44445
API_BRIGHTDATA_USERNAME=[SEU_USUARIO_BRD]
API_BRIGHTDATA_PASSWORD=[SUA_SENHA_BRD]

# Integração Meta API
WHATSAPP_PHONE_ID=[ID_TELEFONE_META]
WHATSAPP_TOKEN=[TOKEN_PRODUCAO_META]
APP_ADMIN_TELEFONE=[TELEFONE_EMERGENCIA_COM_CODIGO_PAIS]
```

2. **Baixe a Imagem Oficial e Inicie o Contêiner:**
   O comando abaixo mapeia a porta HTTP nativa (80) para a interna (8085) e limita a alocação de memória na JVM, prevenindo falhas de `OOMKilled` em instâncias Cloud restritas:

```bash
docker pull magalhaesrafa/monitoramento-precos:latest

docker run -d \
--name monitoramento-api \
--restart unless-stopped \
-p 80:8085 \
--env-file .env \
-e JDK_JAVA_OPTIONS="-Xms256m -Xmx400m" \
magalhaesrafa/monitoramento-precos:latest
```

3. **Acompanhe os Logs:**
```bash
docker logs -f monitoramento-api
```