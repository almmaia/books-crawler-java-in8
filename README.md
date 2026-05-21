# Books Crawler Java

Implementação do desafio técnico para a trilha de Crawler/RPA + IA usando `Java 21` e `Maven`.

O projeto coleta dados do site [Books to Scrape](https://books.toscrape.com/), percorre a paginação do catálogo, extrai informações estruturadas de cada livro e gera arquivos de saída em `JSON` e `CSV`. Como diferenciais, a entrega também inclui persistência opcional em PostgreSQL, automação de navegador para uma página dinâmica e enriquecimento opcional com LLM local.

## Visão geral

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
- `aiInsights`, quando o enriquecimento por IA está habilitado

## Tecnologias utilizadas

- `Java 21`
- `Maven`
- `Jsoup`
- `Jackson`
- `Apache Commons CSV`
- `JUnit 5`
- `MockWebServer`
- `PostgreSQL`, de forma opcional
- `Playwright`, para o bônus de página dinâmica
- `Ollama`, para o bônus de enriquecimento com LLM local

## Estrutura do projeto

```text
src/main/java/com/in8/trainee/crawler
|- cli             -> leitura e validação de argumentos
|- client          -> cliente HTTP com User-Agent e controle de delay
|- config          -> configurações da aplicação, banco e IA
|- http            -> servidor HTTP opcional para execução em container
|- model           -> modelos de domínio
|- parser          -> parse da listagem e da página de detalhe
|- persistence     -> persistência opcional em PostgreSQL
|- service         -> orquestração da coleta
|- writer          -> exportação para JSON e CSV
```

## Como executar sem Docker

Primeiro, gere o artefato com Maven:

```bash
mvn clean package
```

Depois, execute o crawler:

```bash
java -jar target/books-crawler-java-1.0.0.jar
```

Exemplo com parâmetros:

```bash
java -jar target/books-crawler-java-1.0.0.jar --output-dir output --delay-ms 500 --user-agent "books-crawler-java/1.0" --max-books 50
```

Exemplo em modo servidor:

```bash
java -jar target/books-crawler-java-1.0.0.jar --server --port 8080
```

No modo servidor, os endpoints disponíveis são:

- `GET /health`
- `POST /run`

## Como executar com Docker

Antes do build da imagem, gere o artefato com Maven:

```bash
mvn clean package
```

Em seguida, construa a imagem:

```bash
docker build -t books-crawler-java .
```

Suba o container:

```bash
docker run --rm -p 8080:8080 -v ${PWD}/output:/app/output books-crawler-java
```

Depois, dispare uma coleta:

```bash
curl -X POST http://localhost:8080/run
```

## Como executar com Docker Compose

O arquivo `docker-compose.yml` sobe o scraper e um banco PostgreSQL para persistência local.

```bash
docker compose up --build
```

Depois que os serviços estiverem disponíveis:

```bash
curl -X POST http://localhost:8080/run
```

## Estrutura dos dados extraídos

Os arquivos são gravados no diretório `output/` com timestamp UTC.

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

O arquivo `.gitlab-ci.yml` foi dividido em quatro stages. A stage `lint` executa `mvn checkstyle:check` e falha quando encontra violações. A stage `test` executa `mvn test` e falha quando algum teste quebra. A stage `build` gera o artefato, constrói a imagem Docker e faz push para o GitLab Container Registry usando variáveis do CI. Por fim, a stage `deploy` simula um deploy para AWS ECS com `echo` dos comandos e roda apenas na branch `main`.

O pipeline também utiliza cache da pasta `.m2/repository` para reduzir o tempo de builds repetidos.

## Decisões técnicas

Escolhi `Java 21` porque a linguagem oferece bibliotecas maduras, boa legibilidade e um ecossistema forte para testes, parsing e organização em camadas. Para um desafio técnico, isso ajuda a entregar uma solução fácil de ler, simples de manter e tranquila de defender em entrevista.

Usei `Jsoup` porque a fonte de dados é estática e possui HTML consistente. Nesse contexto, browser automation não é necessário para o fluxo principal, e um parser HTML dedicado resolve o problema com menos complexidade operacional.

Separei o parse da listagem e o parse da página de detalhe para reduzir acoplamento e deixar a lógica mais testável. Também mantive a exportação em arquivos separada da orquestração do crawler, o que facilita manutenção e futuras extensões.

A persistência em PostgreSQL foi mantida opcional para não transformar a execução básica em algo dependente de infraestrutura externa. O mesmo critério foi usado no enriquecimento com IA, que fica desacoplado por configuração de ambiente.

## Tratamento de erros e boas práticas

- `User-Agent` explícito nas requisições
- delay configurável entre requisições para respeitar a fonte
- falha explícita para respostas HTTP com status `>= 400`
- preservação de interrupção de thread quando aplicável
- validação de argumentos de entrada
- escrita dos arquivos com `UTF-8`
- execução do container com usuário não root
- testes unitários para parsing e fluxo principal

## O que eu faria com mais tempo

Com mais tempo, eu adicionaria retries com backoff exponencial, logs estruturados, métricas de execução e uma configuração externa mais flexível por profiles ou arquivo `.env`. Também faria uma camada de persistência histórica das execuções para permitir comparação entre coletas e auditoria dos resultados.

Em um cenário mais próximo de produção, eu aprofundaria o tratamento anti-bot, ampliaria a cobertura de testes integrados e adicionaria validações de contrato para os arquivos de saída. Outra evolução natural seria um painel simples para acompanhar execuções e visualizar indicadores do scraper.

O projeto já possui uma primeira versão funcional de enriquecimento com LLM local via `Ollama`. Com mais tempo, eu acrescentaria cache de respostas, score de confiança e prompts especializados por categoria de livro.

## Como usei IA durante o desafio

Usei IA como ferramenta de apoio para acelerar a exploração técnica, revisar alternativas de estrutura e comparar abordagens de implementação. Em todos os casos, as sugestões passaram por validação manual no código, no HTML real da fonte e na execução prática do projeto.

Durante o desafio, a IA ajudou principalmente na organização inicial da arquitetura, na revisão da separação de responsabilidades entre camadas, na comparação de formatos de pipeline GitLab CI e Docker, na análise de seletores CSS para o site e no desenho da ideia de enriquecimento opcional com LLM.

O que funcionou melhor foi a velocidade para gerar alternativas de estrutura e antecipar riscos antes da validação final. O que exigiu validação manual foi o comportamento real do HTML, o ajuste fino do parsing e a confirmação de que testes, build e lint realmente passavam no ambiente.

Os prompts principais utilizados foram os seguintes:

Prompt 1:

```text
Leia este desafio técnico de crawler/RPA + IA e proponha uma arquitetura simples em Java, com separação de responsabilidades, testes, Docker e GitLab CI.
```

Prompt 2:

```text
Com base no HTML do books.toscrape.com, quais seletores CSS são mais robustos para extrair título, preço, disponibilidade, rating e link de detalhe?
```

Prompt 3:

```text
Monte um exemplo de pipeline GitLab CI para um projeto Java com stages lint, test, build e deploy, usando variáveis do registry e cache de dependências.
```

Prompt 4:

```text
Sugira uma forma de usar LLM para transformar descrição livre de um livro em campos estruturados curtos para enriquecer a saída do crawler.
```

## Bônus implementados

- automação de navegador para página dinâmica em `https://quotes.toscrape.com/js/`
- enriquecimento opcional com LLM local para transformar descrição livre em campos estruturados
- cache de dependências Maven no pipeline
- `docker-compose.yml` para subir scraper e banco localmente

## Validação executada

As validações abaixo foram executadas localmente:

- `mvn clean test`
- `mvn checkstyle:check`
- `mvn -DskipTests package`
- execução real do JAR em modo CLI
- `docker compose config`
- `npm install`
- `npm run dynamic:scrape`
- execução real do enriquecimento com `Ollama`
- `docker build -t books-crawler-java:test .`
- execução do container com validação dos endpoints `GET /health` e `POST /run`

Para repetir a validação principal no Windows PowerShell:

```powershell
.\scripts\validate.ps1
```

## Comandos úteis

```bash
mvn test
mvn checkstyle:check
mvn clean package
java -jar target/books-crawler-java-1.0.0.jar --server --port 8080
```
