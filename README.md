# Books Crawler Java

Implementacao do desafio tecnico de Crawler/RPA + IA usando `Java 21` e `Maven`.

O projeto coleta dados do site [Books to Scrape](https://books.toscrape.com/), percorre toda a paginacao, extrai dados estruturados dos livros e exporta o resultado em `JSON` e `CSV`. Como diferencial, tambem suporta persistencia opcional em PostgreSQL e um modo HTTP para execucao em container.

Tambem inclui os dois bonus tecnicos mais fortes do desafio:

- browser automation para uma pagina dinamica publica
- uso de LLM para enriquecer descricoes de livros com dados estruturados

## Stack

- `Java 21`
- `Maven`
- `Jsoup` para parsing de HTML
- `Jackson` para serializacao JSON
- `Apache Commons CSV` para exportacao CSV
- `JUnit 5` + `MockWebServer` para testes
- `PostgreSQL` opcional para persistencia adicional
- `Playwright` para browser automation do bonus dinamico
- `Ollama` opcional para enrichment com LLM local

## O que o scraper extrai

Para cada livro, o crawler coleta:

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
- `aiInsights` opcional

## Estrutura do projeto

```text
src/main/java/com/in8/trainee/crawler
|- cli             -> leitura de argumentos
|- client          -> cliente HTTP com User-Agent e delay entre requisicoes
|- config          -> configuracoes da aplicacao e do banco
|- http            -> servidor HTTP opcional para uso em container
|- model           -> contratos de dados
|- parser          -> parse de listagem e detalhe dos livros
|- persistence     -> persistencia opcional em PostgreSQL
|- service         -> orquestracao da coleta
|- writer          -> exportacao para JSON e CSV
```

## Como rodar localmente

### Sem Docker

Build e execucao:

```bash
mvn clean package
java -jar target/books-crawler-java-1.0.0.jar
```

Com parametros opcionais:

```bash
java -jar target/books-crawler-java-1.0.0.jar --output-dir output --delay-ms 500 --user-agent "books-crawler-java/1.0" --max-books 50
```

Com enrichment por IA via `Ollama` local:

```bash
set BOOKS_AI_ENABLED=true
set BOOKS_AI_MODEL=qwen2.5-coder:7b
set BOOKS_AI_BASE_URL=http://127.0.0.1:11434/v1
java -jar target/books-crawler-java-1.0.0.jar --output-dir output-ai --max-books 5
```

Modo servidor HTTP:

```bash
java -jar target/books-crawler-java-1.0.0.jar --server --port 8080
```

Endpoints do modo servidor:

- `GET /health` retorna status da aplicacao
- `POST /run` executa uma nova coleta

### Com Docker

Build da imagem:

```bash
mvn clean package
docker build -t books-crawler-java .
```

Observacao:

- o `Dockerfile` copia o artefato gerado pelo Maven, entao execute `mvn clean package` antes do `docker build`

Subir o container em modo servidor:

```bash
docker run --rm -p 8080:8080 -v ${PWD}/output:/app/output books-crawler-java
```

Disparar a coleta:

```bash
curl -X POST http://localhost:8080/run
```

### Com Docker Compose e PostgreSQL

```bash
docker compose up --build
```

Depois de subir os servicos:

```bash
curl -X POST http://localhost:8080/run
```

Nesse modo, alem de gerar `JSON` e `CSV`, o scraper persiste os livros na tabela `books` do PostgreSQL.

Se quiser limitar a quantidade de livros em ambiente container para testes:

```bash
docker run --rm -p 8080:8080 -e BOOKS_MAX_BOOKS=5 -v ${PWD}/output:/app/output books-crawler-java
```

## Bonus de browser automation

Foi adicionado um scraper bonus para uma pagina dinamica publica carregada com JavaScript:

- fonte: `https://quotes.toscrape.com/js/`
- tecnologia: `Playwright`
- saida: `JSON` e `CSV` em `output-dynamic/`

Instalacao:

```bash
npm install
npx playwright install chromium
```

Execucao:

```bash
npm run dynamic:scrape
```

## Saidas geradas

Os arquivos sao gravados no diretorio `output/` com timestamp UTC:

- `books-YYYYMMDD-HHMMSS.json`
- `books-YYYYMMDD-HHMMSS.csv`

### Schema do JSON

O JSON e uma lista de objetos com o formato:

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

Colunas exportadas:

```text
title,product_page_url,image_url,category,upc,price_gbp,rating,in_stock,availability,description,ai_short_summary,ai_themes,ai_recommended_audience,ai_tone,ai_content_warnings
```

## Pipeline GitLab CI/CD

O arquivo `.gitlab-ci.yml` foi organizado em quatro stages:

- `lint`: executa `mvn checkstyle:check` e falha se houver problemas de estilo
- `test`: executa `mvn test` e falha se algum teste quebrar
- `build`: constroi a imagem Docker e faz push para o `GitLab Container Registry`
- `deploy`: simula o deploy para AWS ECS com `echo` dos comandos, rodando apenas na branch `main`

Diferencial aplicado:

- cache da pasta `.m2/repository` para acelerar builds futuros

## Validacao executada

Os seguintes testes e validacoes foram executados localmente:

- `mvn clean test`
- `mvn checkstyle:check`
- `mvn -DskipTests package`
- `java -jar target/books-crawler-java-1.0.0.jar --delay-ms 0 --max-books 2 --output-dir output-validation`
- `docker compose config`
- `npm install`
- `npm run dynamic:scrape`
- `java -jar target/books-crawler-java-1.0.0.jar --delay-ms 0 --max-books 1 --output-dir output-ai-validation` com `Ollama`
- `docker build -t books-crawler-java:test .`
- `docker run --rm -d --name books-crawler-java-test -p 8080:8080 -e BOOKS_MAX_BOOKS=2 -v <host-output>:/app/output books-crawler-java:test`
- `GET /health` e `POST /run` contra o container em execucao

Para repetir a maior parte da validacao automaticamente no Windows PowerShell:

```powershell
.\scripts\validate.ps1
```

## Decisoes tecnicas

- Escolhi `Java 21` por ser a linguagem com melhor equilibrio entre clareza, maturidade de bibliotecas e facilidade de defesa tecnica em entrevista.
- Usei `Jsoup` porque o site e estatico e a biblioteca resolve muito bem seletores, navegacao de elementos e HTML imperfeito.
- Separei parser de listagem e parser de pagina de detalhe para manter responsabilidade unica e facilitar teste.
- Mantive delay entre requisicoes e `User-Agent` explicito para respeitar a fonte.
- A persistencia no banco foi deixada opcional para nao acoplar a execucao basica a infraestrutura externa.
- O modo HTTP existe principalmente para containerizacao, health check e execucao sob orquestracao.
- O enrichment com LLM foi deixado opcional e desacoplado por ambiente para manter o crawler principal simples, mas ainda demonstrar uso real de IA em um campo livre.
- O bonus de browser automation foi separado em um script proprio para nao misturar scraping estatico e dinamico na mesma responsabilidade.

## Tratamento de erros e boas praticas

- Falha explicita para respostas HTTP com status `>= 400`
- Interrupcao de thread preservada quando aplicavel
- Validacao de argumentos de entrada
- Escrita de arquivos com encoding `UTF-8`
- Usuario nao-root no container
- Testes cobrindo parsing e fluxo de coleta

## O que eu faria com mais tempo

- retries com backoff exponencial
- metricas e logs estruturados
- configuracao externa via arquivo `.env` ou profiles
- persistencia historica com versionamento de execucoes
- anti-bot handling para fontes mais restritivas
- cobertura maior de testes integrados e contratos de saida
- dashboard simples para acompanhar execucoes
- enrichment com LLM para normalizacao semantica da descricao dos livros

Observacao:

- o projeto ja contem uma primeira versao funcional desse enrichment com LLM local via `Ollama`
- com mais tempo, eu acrescentaria cache de respostas, score de confianca e prompts por categoria

## Como usei IA durante o desafio

Usei IA como apoio para acelerar etapas operacionais e revisar direcao tecnica, mantendo validacao manual das decisoes.

Aplicacoes principais:

- acelerar o esqueleto inicial do projeto
- revisar a separacao de responsabilidades entre camadas
- validar ideias de pipeline GitLab e Docker multi-stage
- revisar seletores e edge cases do parsing

O que funcionou bem:

- gerar rapidamente alternativas de estrutura
- comparar abordagens de organizacao
- apontar melhorias de robustez antes da validacao final

O que exigiu validacao manual:

- conferir o HTML real do site
- ajustar parsing da descricao e disponibilidade
- validar testes, build e lint de verdade no ambiente

### Prompts usados

Prompt 1:

```text
Leia este desafio tecnico de crawler/RPA + IA e proponha uma arquitetura simples em Java, com separacao de responsabilidades, testes, Docker e GitLab CI.
```

Uso:

- definir a estrutura inicial do projeto
- decidir as camadas principais

Prompt 2:

```text
Com base no HTML do books.toscrape.com, quais seletores CSS sao mais robustos para extrair titulo, preco, disponibilidade, rating e link de detalhe?
```

Uso:

- acelerar a escolha dos seletores
- revisar edge cases de parsing

Prompt 3:

```text
Monte um exemplo de pipeline GitLab CI para um projeto Java com stages lint, test, build e deploy, usando variaveis do registry e cache de dependencias.
```

Uso:

- estruturar o `.gitlab-ci.yml`
- revisar boas praticas de cache

Prompt 4:

```text
Sugira uma forma de usar LLM para transformar descricao livre de um livro em campos estruturados curtos para enriquecer a saida do crawler.
```

Uso:

- definir os campos do enrichment por IA
- desenhar o prompt de extracao estruturada

### O que funcionou e o que nao funcionou

Funcionou bem:

- usar IA para acelerar a arquitetura inicial
- usar IA para revisar seletores e estrutura do pipeline
- usar IA para desenhar um enrichment opcional por LLM

Nao funcionou sozinho, sem validacao:

- algumas sugestoes de parsing precisaram ser conferidas contra o HTML real
- o enrichment com modelo local precisou ajuste de timeout e tamanho da resposta
- a documentacao gerada a partir de IA precisou revisao manual para ficar fiel ao PDF

## Comandos uteis

```bash
mvn test
mvn checkstyle:check
mvn clean package
java -jar target/books-crawler-java-1.0.0.jar --server --port 8080
```
