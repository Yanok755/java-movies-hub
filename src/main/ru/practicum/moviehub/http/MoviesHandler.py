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
            // Возвращаем пустой JSON массив
            sendJson(exchange, 200, new String[]{}); // Пустой массив
        } else {
            exchange.sendResponseHeaders(405, -1); // Method Not Allowed
        }
    }
}
