package com.in8.trainee.crawler.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.in8.trainee.crawler.model.CatalogPage;
import java.net.URI;
import org.junit.jupiter.api.Test;

class CatalogPageParserTest {

    @Test
    void shouldParseCatalogPage() {
        String html = """
                <html>
                <body>
                  <article class="product_pod">
                    <div class="image_container">
                      <a href="a-book/index.html"><img src="../media/cache/image.jpg" class="thumbnail" alt="A Book"></a>
                    </div>
                    <p class="star-rating Three"></p>
                    <h3><a href="a-book/index.html" title="A Book">A Book</a></h3>
                    <div class="product_price">
                      <p class="price_color">£12.99</p>
                      <p class="instock availability">In stock</p>
                    </div>
                  </article>
                  <li class="next"><a href="page-2.html">next</a></li>
                </body>
                </html>
                """;

        CatalogPageParser parser = new CatalogPageParser();

        CatalogPage page = parser.parse(URI.create("https://books.toscrape.com/catalogue/page-1.html"), html);

        assertEquals(1, page.books().size());
        assertEquals("A Book", page.books().getFirst().title());
        assertEquals(3, page.books().getFirst().rating());
        assertTrue(page.nextPageUrl().isPresent());
    }
}
