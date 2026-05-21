# Books Crawler Java

Implementacao do desafio tecnico para a trilha de Crawler/RPA + IA usando `Java 21` e `Maven`.

O projeto coleta dados do site [Books to Scrape](https://books.toscrape.com/), percorre a paginacao do catalogo, extrai informacoes estruturadas de cada livro e gera arquivos de saida em `JSON` e `CSV`. Como diferenciais, a entrega tambem inclui persistencia opcional em PostgreSQL, browser automation para uma pagina dinamica e enrichment opcional com LLM local.

## Visao geral

O crawler extrai os seguintes campos para cada livro:

- `title`
- `productPageUrl`
- `imageUrl`
- `category`
- `upc`
- `priceGbp`
- `rating`
- `inStock`
- `availability`
- `description`
- `aiInsights` quando o enrichment por IA esta habilitado

## Tecnologias utilizadas

- `Java 21`
- `Maven`
- `Jsoup`
- `Jackson`
- `Apache Commons CSV`
- `JUnit 5`
- `MockWebServer`
- `PostgreSQL` opcional
- `Playwright` para o bonus de pagina dinamica
- `Ollama` para o bonus de enrichment com LLM local

## Estrutura do projeto

```text
src/main/java/com/in8/trainee/crawler
|- cli             -> leitura e validacao de argumentos
|- client          -> cliente HTTP com User-Agent e controle de delay
|- config          -> configuracoes da aplicacao, banco e IA
|- http            -> servidor HTTP opcional para execucao em container
|- model           -> modelos de dominio
|- parser          -> parse da listagem e da pagina de detalhe
|- persistence     -> persistencia opcional em PostgreSQL
|- service         -> orquestracao da coleta
|- writer          -> exportacao para JSON e CSV
```

## Como executar sem Docker

Compile e gere o artefato:

```bash
mvn clean package
```

Execute o crawler:

```bash
java -jar target/books-crawler-java-1.0.0.jar
```

Exemplo com parametros:

```bash
java -jar target/books-crawler-java-1.0.0.jar --output-dir output --delay-ms 500 --user-agent "books-crawler-java/1.0" --max-books 50
```

Exemplo em modo servidor:

```bash
java -jar target/books-crawler-java-1.0.0.jar --server --port 8080
```

Endpoints disponiveis no modo servidor:

- `GET /health`
- `POST /run`

## Como executar com Docker

Primeiro gere o artefato com Maven:

```bash
mvn clean package
```

Depois construa a imagem:

```bash
docker build -t books-crawler-java .
```

Suba o container:

```bash
docker run --rm -p 8080:8080 -v ${PWD}/output:/app/output books-crawler-java
```

Dispare uma coleta:

```bash
curl -X POST http://localhost:8080/run
```

## Como executar com Docker Compose

O arquivo `docker-compose.yml` sobe o scraper e um banco PostgreSQL para persistencia local.

```bash
docker compose up --build
```

Depois que os servicos estiverem disponiveis:

```bash
curl -X POST http://localhost:8080/run
```

## Estrutura dos dados extraidos

Os arquivos sao gravados no diretorio `output/` com timestamp UTC.

Arquivos gerados:

- `books-YYYYMMDD-HHMMSS.json`
- `books-YYYYMMDD-HHMMSS.csv`

### Schema do JSON

```json
[
  {
    "title": "A Light in the Attic",
    "productPageUrl": "https://books.toscrape.com/catalogue/a-light-in-the-attic_1000/index.html",
    "imageUrl": "https://books.toscrape.com/media/cache/fe/72/fe72f0532301ec28892ae79a629a293c.jpg",
    "category": "Poetry",
    "upc": "a897fe39b1053632",
    "priceGbp": 51.77,
    "rating": 3,
    "inStock": true,
    "availability": "In stock (22 available)",
    "description": "It's hard to imagine a world without A Light in the Attic...",
    "aiInsights": {
      "shortSummary": "A collection of humorous and imaginative poetry for readers of all ages.",
      "themes": ["Humor", "Creativity", "Childhood"],
      "recommendedAudience": "All ages",
      "tone": "Humorous, Lighthearted",
      "contentWarnings": []
    }
  }
]
```

### Schema do CSV

```text
title,product_page_url,image_url,category,upc,price_gbp,rating,in_stock,availability,description,ai_short_summary,ai_themes,ai_recommended_audience,ai_tone,ai_content_warnings
```

## Pipeline GitLab CI/CD

O arquivo `.gitlab-ci.yml` foi dividido em quatro stages:

- `lint`: executa `mvn checkstyle:check` e falha quando encontra violacoes
- `test`: executa `mvn test` e falha quando algum teste quebra
- `build`: gera o artefato, constroi a imagem Docker e faz push para o GitLab Container Registry usando variaveis do CI
- `deploy`: simula um deploy para AWS ECS com `echo` dos comandos e roda apenas na branch `main`

O pipeline tambem usa cache da pasta `.m2/repository` para reduzir o tempo de builds repetidos.

## Decisoes tecnicas

Escolhi `Java 21` porque a linguagem oferece bibliotecas maduras, boa legibilidade e um ecossistema forte para testes, parsing e organizacao em camadas. Para um desafio tecnico, isso ajuda a entregar uma solucao facil de ler e simples de defender.

Usei `Jsoup` porque a fonte de dados e estatica e possui HTML consistente. Nesse contexto, browser automation nao e necessario para o fluxo principal, e um parser HTML dedicado resolve o problema com menos complexidade operacional.

Separei o parse da listagem e o parse da pagina de detalhe para reduzir acoplamento e deixar a logica mais testavel. Tambem mantive a exportacao em arquivos separada da orquestracao do crawler, o que facilita manutencao e extensao futura.

A persistencia em PostgreSQL foi mantida opcional para nao transformar a execucao basica em algo dependente de infraestrutura externa. O mesmo criterio foi usado no enrichment com IA, que fica desacoplado por configuracao de ambiente.

## Tratamento de erros e boas praticas

- `User-Agent` explicito nas requisicoes
- delay configuravel entre requisicoes para respeitar a fonte
- falha explicita para respostas HTTP com status `>= 400`
- preservacao de interrupcao de thread quando aplicavel
- validacao de argumentos de entrada
- escrita dos arquivos com `UTF-8`
- execucao do container com usuario nao root
- testes unitarios para parsing e fluxo principal

## O que eu faria com mais tempo

Com mais tempo, eu adicionaria retries com backoff exponencial, logs estruturados, metricas de execucao e configuracao externa mais flexivel por profiles ou arquivo `.env`. Tambem faria uma camada de persistencia historica das execucoes para permitir comparacao entre coletas e auditoria dos resultados.

Em um cenario mais proximo de producao, eu aprofundaria tratamento anti-bot, ampliaria a cobertura de testes integrados e adicionaria validacoes de contrato para os arquivos de saida. Outra evolucao natural seria um painel simples para acompanhar execucoes e visualizar indicadores do scraper.

O projeto ja possui uma primeira versao funcional de enrichment com LLM local via `Ollama`. Com mais tempo, eu acrescentaria cache de respostas, score de confianca e prompts especializados por categoria de livro.

## Como usei IA durante o desafio

Usei IA como ferramenta de apoio para acelerar exploracao tecnica, revisar alternativas de estrutura e validar abordagens de implementacao. Nenhuma sugestao foi aceita sem verificacao no codigo, no HTML real da fonte e na execucao do projeto.

Os usos principais foram:

- estruturar o esqueleto inicial do projeto
- revisar separacao de responsabilidades entre camadas
- comparar abordagens de pipeline GitLab CI e Docker
- revisar seletores e edge cases de parsing
- desenhar a ideia do enrichment opcional por LLM

O que funcionou bem foi a velocidade para gerar alternativas de estrutura e apontar riscos antes da validacao final. O que exigiu validacao manual foi o comportamento real do HTML, o ajuste fino do parsing e a confirmacao de que testes, build e lint realmente passavam no ambiente.

Prompts utilizados durante o desafio:

Prompt 1:

```text
Leia este desafio tecnico de crawler/RPA + IA e proponha uma arquitetura simples em Java, com separacao de responsabilidades, testes, Docker e GitLab CI.
```

Prompt 2:

```text
Com base no HTML do books.toscrape.com, quais seletores CSS sao mais robustos para extrair titulo, preco, disponibilidade, rating e link de detalhe?
```

Prompt 3:

```text
Monte um exemplo de pipeline GitLab CI para um projeto Java com stages lint, test, build e deploy, usando variaveis do registry e cache de dependencias.
```

Prompt 4:

```text
Sugira uma forma de usar LLM para transformar descricao livre de um livro em campos estruturados curtos para enriquecer a saida do crawler.
```

## Bonus implementados

- browser automation para pagina dinamica em `https://quotes.toscrape.com/js/`
- enrichment opcional com LLM local para transformar descricao livre em campos estruturados
- cache de dependencias Maven no pipeline
- `docker-compose.yml` para subir scraper e banco localmente

## Validacao executada

As validacoes abaixo foram executadas localmente:

- `mvn clean test`
- `mvn checkstyle:check`
- `mvn -DskipTests package`
- execucao real do JAR em modo CLI
- `docker compose config`
- `npm install`
- `npm run dynamic:scrape`
- execucao real do enrichment com `Ollama`
- `docker build -t books-crawler-java:test .`
- execucao do container com validacao dos endpoints `GET /health` e `POST /run`

Para repetir a validacao principal no Windows PowerShell:

```powershell
.\scripts\validate.ps1
```

## Comandos uteis

```bash
mvn test
mvn checkstyle:check
mvn clean package
java -jar target/books-crawler-java-1.0.0.jar --server --port 8080
```
