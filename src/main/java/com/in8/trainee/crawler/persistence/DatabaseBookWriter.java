package com.in8.trainee.crawler.persistence;

import com.in8.trainee.crawler.config.DatabaseConfig;
import com.in8.trainee.crawler.model.Book;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

public final class DatabaseBookWriter {

    public void persist(final List<Book> books, final DatabaseConfig databaseConfig) {
        try (Connection connection = DriverManager.getConnection(
                databaseConfig.jdbcUrl(),
                databaseConfig.username(),
                databaseConfig.password()
        )) {
            connection.setAutoCommit(false);
            createTableIfNeeded(connection);
            upsertBooks(connection, books);
            connection.commit();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to persist books in the database.", exception);
        }
    }

    private void createTableIfNeeded(final Connection connection) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS books (
                    upc VARCHAR(64) PRIMARY KEY,
                    title TEXT NOT NULL,
                    product_page_url TEXT NOT NULL,
                    image_url TEXT NOT NULL,
                    category VARCHAR(255) NOT NULL,
                    price_gbp NUMERIC(10, 2) NOT NULL,
                    rating INTEGER NOT NULL,
                    in_stock BOOLEAN NOT NULL,
                    availability TEXT NOT NULL,
                    description TEXT,
                    ai_short_summary TEXT,
                    ai_themes TEXT,
                    ai_recommended_audience TEXT,
                    ai_tone TEXT,
                    ai_content_warnings TEXT
                )
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        }
    }

    private void upsertBooks(final Connection connection, final List<Book> books) throws SQLException {
        String sql = """
                INSERT INTO books (
                    upc,
                    title,
                    product_page_url,
                    image_url,
                    category,
                    price_gbp,
                    rating,
                    in_stock,
                    availability,
                    description,
                    ai_short_summary,
                    ai_themes,
                    ai_recommended_audience,
                    ai_tone,
                    ai_content_warnings
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (upc) DO UPDATE SET
                    title = EXCLUDED.title,
                    product_page_url = EXCLUDED.product_page_url,
                    image_url = EXCLUDED.image_url,
                    category = EXCLUDED.category,
                    price_gbp = EXCLUDED.price_gbp,
                    rating = EXCLUDED.rating,
                    in_stock = EXCLUDED.in_stock,
                    availability = EXCLUDED.availability,
                    description = EXCLUDED.description,
                    ai_short_summary = EXCLUDED.ai_short_summary,
                    ai_themes = EXCLUDED.ai_themes,
                    ai_recommended_audience = EXCLUDED.ai_recommended_audience,
                    ai_tone = EXCLUDED.ai_tone,
                    ai_content_warnings = EXCLUDED.ai_content_warnings
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Book book : books) {
                bind(statement, book);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void bind(final PreparedStatement statement, final Book book) throws SQLException {
        statement.setString(1, book.upc());
        statement.setString(2, book.title());
        statement.setString(3, book.productPageUrl().toString());
        statement.setString(4, book.imageUrl().toString());
        statement.setString(5, book.category());
        statement.setBigDecimal(6, book.priceGbp().setScale(2));
        statement.setInt(7, book.rating());
        statement.setBoolean(8, book.inStock());
        statement.setString(9, book.availability());

        if (book.description().isBlank()) {
            statement.setNull(10, Types.VARCHAR);
        } else {
            statement.setString(10, book.description());
        }

        if (book.aiInsights() == null) {
            statement.setNull(11, Types.VARCHAR);
            statement.setNull(12, Types.VARCHAR);
            statement.setNull(13, Types.VARCHAR);
            statement.setNull(14, Types.VARCHAR);
            statement.setNull(15, Types.VARCHAR);
            return;
        }

        statement.setString(11, book.aiInsights().shortSummary());
        statement.setString(12, String.join(" | ", book.aiInsights().themes()));
        statement.setString(13, book.aiInsights().recommendedAudience());
        statement.setString(14, book.aiInsights().tone());
        statement.setString(15, String.join(" | ", book.aiInsights().contentWarnings()));
    }
}
