package com.in8.trainee.crawler;

import com.in8.trainee.crawler.ai.AiBookEnricher;
import com.in8.trainee.crawler.cli.AppOptions;
import com.in8.trainee.crawler.cli.CommandLineParser;
import com.in8.trainee.crawler.client.HtmlFetcher;
import com.in8.trainee.crawler.config.AppConfig;
import com.in8.trainee.crawler.http.ScraperHttpServer;
import com.in8.trainee.crawler.service.CrawlOrchestrator;
import com.in8.trainee.crawler.service.CrawlerService;
import com.in8.trainee.crawler.writer.BookExporter;
import java.net.http.HttpClient;
import java.time.Clock;

public final class Application {

    private Application() {
    }

    public static void main(final String[] args) throws Exception {
        System.setProperty("jdk.httpclient.allowRestrictedHeaders", "host");
        AppOptions options = CommandLineParser.parse(args);
        AppConfig config = AppConfig.from(options);

        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HtmlFetcher htmlFetcher = new HtmlFetcher(
                httpClient,
                config.userAgent(),
                config.delayMillis(),
                Clock.systemUTC()
        );

        CrawlerService crawlerService = new CrawlerService(
                htmlFetcher,
                config.baseUrl(),
                config.aiConfig().map(aiConfig -> new AiBookEnricher(httpClient, aiConfig))
        );
        BookExporter exporter = new BookExporter();
        CrawlOrchestrator orchestrator = new CrawlOrchestrator(crawlerService, exporter, config);

        if (options.serverMode()) {
            new ScraperHttpServer(orchestrator, config).start();
            System.out.println("HTTP server listening on port " + config.port() + ".");
            return;
        }

        var result = orchestrator.run();
        System.out.println("Crawl completed successfully.");
        System.out.println("Books collected: " + result.books().size());
        System.out.println("JSON output: " + result.jsonPath());
        System.out.println("CSV output: " + result.csvPath());
        System.out.println("Database persistence: " + (result.databasePersisted() ? "enabled" : "disabled"));
        System.out.println("AI enrichment: " + (result.aiEnrichmentEnabled() ? "enabled" : "disabled"));
    }
}
