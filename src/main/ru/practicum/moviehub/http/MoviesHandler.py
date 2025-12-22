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

    private void handleMoviesEndpoint(HttpExchange exchange, String method) throws IOException {
        switch (method) {
            case "GET":
                handleGetMovies(exchange);
                break;
            case "POST":
                handleAddMovie(exchange);
                break;
            default:
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
        }
    }

    private void handleMovieByIdEndpoint(HttpExchange exchange, String method, String path) throws IOException {
        try {
            String[] parts = path.split("/");
            if (parts.length < 3) {
                sendText(exchange, 400, "Invalid URL");
                return;
            }

            int id = Integer.parseInt(parts[2]);

            switch (method) {
                case "GET":
                    handleGetMovieById(exchange, id);
                    break;
                case "DELETE":
                    handleDeleteMovie(exchange, id);
                    break;
                default:
                    exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            }
        } catch (NumberFormatException e) {
            sendText(exchange, 400, "Invalid movie ID");
        }
    }

    private void handleGetMovies(HttpExchange exchange) throws IOException {
        List<Movie> movies = moviesStore.getAllMovies();
        sendJson(exchange, 200, movies);
    }

    private void handleAddMovie(HttpExchange exchange) throws IOException {
        try {
            String body = readRequestBody(exchange);
            Movie movie = gson.fromJson(body, Movie.class);

            // Валидация
            if (movie == null || movie.getTitle() == null || movie.getTitle().trim().isEmpty()) {
                sendText(exchange, 400, "Invalid movie data: title is required");
                return;
            }

            if (movie.getYear() <= 0 || movie.getDuration() <= 0) {
                sendText(exchange, 400, "Invalid movie data: year and duration must be positive");
                return;
            }

            Movie savedMovie = moviesStore.addMovie(movie);
            sendJson(exchange, 201, savedMovie);

        } catch (JsonSyntaxException e) {
            sendText(exchange, 400, "Invalid JSON format");
        }
    }

    private void handleGetMovieById(HttpExchange exchange, int id) throws IOException {
        Movie movie = moviesStore.getMovieById(id);
        if (movie == null) {
            sendText(exchange, 404, "Movie not found");
        } else {
            sendJson(exchange, 200, movie);
        }
    }

    private void handleDeleteMovie(HttpExchange exchange, int id) throws IOException {
        boolean deleted = moviesStore.deleteMovie(id);
        if (deleted) {
            sendNoContent(exchange);
        } else {
            sendText(exchange, 404, "Movie not found");
        }
    }

    // Метод для чтения тела запроса
    private String readRequestBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    }
}
