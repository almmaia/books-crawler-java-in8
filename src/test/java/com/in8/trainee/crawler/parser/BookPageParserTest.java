package com.in8.trainee.crawler.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.in8.trainee.crawler.model.Book;
import com.in8.trainee.crawler.model.BookSummary;
import java.math.BigDecimal;
import java.net.URI;
import org.junit.jupiter.api.Test;

class BookPageParserTest {

    @Test
    void shouldParseBookDetailPage() {
        String html = """
                <html>
                <body>
                  <ul class="breadcrumb">
                    <li><a href="/">Home</a></li>
                    <li><a href="/books">Books</a></li>
                    <li><a href="/travel">Travel</a></li>
                  </ul>
                  <div id="product_gallery">
                    <img src="../../media/cache/image.jpg" alt="A Book" />
                  </div>
                  <div id="product_description"><h2>Product Description</h2></div>
                  <p>Concise description.</p>
                  <table class="table table-striped">
                    <tr><th>UPC</th><td>a123</td></tr>
                    <tr><th>Availability</th><td>In stock (3 available)</td></tr>
                  </table>
                </body>
                </html>
                """;

        BookSummary summary = new BookSummary(
                "A Book",
                URI.create("https://books.toscrape.com/catalogue/a-book/index.html"),
                URI.create("https://books.toscrape.com/media/cache/image.jpg"),
                new BigDecimal("12.99"),
                4,
                true,
                "In stock"
        );

        BookPageParser parser = new BookPageParser();
        Book book = parser.parse(summary, html);

        assertEquals("Travel", book.category());
        assertEquals("a123", book.upc());
        assertEquals("Concise description.", book.description());
        assertEquals("In stock (3 available)", book.availability());
    }
}
