package com.in8.trainee.crawler.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.in8.trainee.crawler.model.Book;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class JsonBookWriter {

    private final ObjectMapper objectMapper;

    public JsonBookWriter() {
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void write(final List<Book> books, final Path path) throws IOException {
        objectMapper.writeValue(path.toFile(), books);
    }
}
