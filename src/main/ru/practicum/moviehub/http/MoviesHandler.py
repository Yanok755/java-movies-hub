package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.util.List;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore moviesStore;
    private final Gson gson;

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
        this.gson = new Gson();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            
            if ("/movies".equals(path)) {
                handleMoviesEndpoint(exchange, method);
            } else if (path.startsWith("/movies/")) {
                handleMovieByIdEndpoint(exchange, method, path);
            } else {
                sendText(exchange, 404, "Not Found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendText(exchange, 500, "Internal Server Error");
        }
    }

    // ... остальные методы handleGetMovies, handleAddMovie и т.д.
}
