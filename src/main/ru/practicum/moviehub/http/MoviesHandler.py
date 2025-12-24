package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore moviesStore;

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if ("GET".equalsIgnoreCase(method)) {
            handleGet(exchange);
        } else if ("POST".equalsIgnoreCase(method)) {
            handlePost(exchange);
        } else {
            exchange.sendResponseHeaders(405, -1); // Method Not Allowed
        }
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        sendJson(exchange, 200, moviesStore.getAllMovies());
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        // TODO: Реализовать в следующих этапах
        exchange.sendResponseHeaders(501, -1); // Not Implemented
    }
}
