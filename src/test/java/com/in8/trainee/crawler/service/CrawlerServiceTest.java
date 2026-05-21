package com.in8.trainee.crawler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.in8.trainee.crawler.client.HtmlFetcher;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

class CrawlerServiceTest {

    @Test
    void shouldCrawlCatalogAndBookDetailPages() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setBody("""
                    <html>
                    <body>
                      <article class="product_pod">
                        <div class="image_container">
                          <a href="book-1/index.html"><img src="../media/cache/book-1.jpg" class="thumbnail" alt="Book 1"></a>
                        </div>
                        <p class="star-rating Five"></p>
                        <h3><a href="book-1/index.html" title="Book 1">Book 1</a></h3>
                        <div class="product_price">
                          <p class="price_color">£20.00</p>
                          <p class="instock availability">In stock</p>
                        </div>
                      </article>
                    </body>
                    </html>
                    """));
            server.enqueue(new MockResponse().setBody("""
                    <html>
                    <body>
                      <ul class="breadcrumb">
                        <li><a href="/">Home</a></li>
                        <li><a href="/books">Books</a></li>
                        <li><a href="/fiction">Fiction</a></li>
                      </ul>
                      <article class="product_page">
                        <p>Detailed description.</p>
                      </article>
                      <table class="table table-striped">
                        <tr><th>UPC</th><td>upc-1</td></tr>
                        <tr><th>Availability</th><td>In stock (5 available)</td></tr>
                      </table>
                    </body>
                    </html>
                    """));
            server.start();

            HtmlFetcher fetcher = new HtmlFetcher(
                    HttpClient.newHttpClient(),
                    "test-agent",
                    0L,
                    Clock.systemUTC()
            );
            CrawlerService crawlerService = new CrawlerService(
                    fetcher,
                    URI.create(server.url("/").toString()),
                    java.util.Optional.empty()
            );

            var books = crawlerService.crawlAllBooks(Integer.MAX_VALUE);

            assertEquals(1, books.size());
            assertEquals("upc-1", books.getFirst().upc());
        }
    }
}
