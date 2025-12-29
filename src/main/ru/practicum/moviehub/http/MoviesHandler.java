package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore moviesStore;

    public MoviesHandler(MoviesStore moviesStore) {
        super();
        this.moviesStore = moviesStore;
    }

    public MoviesHandler(MoviesStore moviesStore, Gson gson) {
        super(gson);
        this.moviesStore = moviesStore;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            switch (method) {
                case "GET":
                    if (path.equals("/movies")) {
                        handleGetMovies(exchange);
                    } else if (isMovieByIdPath(path)) {
                        handleGetMovieById(exchange);
                    } else {
                        sendError(exchange, 404, "Неверный путь");
                    }
                    break;

                case "POST":
                    if (path.equals("/movies")) {
                        handleAddMovies(exchange);
                    } else {
                        sendError(exchange, 404, "Неверный путь");
                    }
                    break;

                case "DELETE":
                    if (isMovieByIdPath(path)) {
                        handleDeleteMovie(exchange);
                    } else {
                        sendError(exchange, 404, "Неверный путь");
                    }
                    break;

                default:
                    sendError(exchange, 405, "Method Not Allowed");
            }
        } catch (Exception e) {
            sendError(exchange, 500, "Ошибка сервера");
        }
    }

    private boolean isMovieByIdPath(String path) {
        return path.startsWith("/movies/") && path.length() > "/movies/".length();
    }

    private void handleGetMovies(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        List<Movie> movies = moviesStore.getAllMovie();

        if (query != null && query.contains("sortBy=name")) {
            movies.sort(Comparator.comparing(Movie::getName));
        }
        sendJson(exchange, 200, movies);
    }

    private void handleGetMovieById(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String idStr = path.substring(path.lastIndexOf("/") + 1);

        try {
            int id = Integer.parseInt(idStr);
            Movie movie = moviesStore.getMovieById(id);

            if (movie != null) {
                sendJson(exchange, 200, movie);
            } else {
                sendError(exchange, 404, "Фильм с id " + id + " не найден");
            }
        } catch (NumberFormatException e) {
            sendError(exchange, 400, "Некорректный id фильма");
        }
    }

    private void handleAddMovies(HttpExchange exchange) throws IOException {
        if (!"application/json".equals(exchange.getRequestHeaders().getFirst("Content-Type"))) {
            sendError(exchange, 415, "Требуется Content-Type: application/json");
            return;
        }

        try {
            String requestBody = readRequestBody(exchange);
            Movie movie = parseJson(requestBody, Movie.class);

            if (movie.getName() == null || movie.getName().trim().isEmpty()) {
                sendError(exchange, 400, "Название фильма обязательно");
                return;
            }

            if (movie.getDescription() == null || movie.getDescription().trim().isEmpty()) {
                sendError(exchange, 400, "Описание фильма обязательно");
                return;
            }

            if (movie.getDuration() <= 0) {
                sendError(exchange, 400, "Длительность должна быть положительным числом");
                return;
            }

            Movie addedMovie = moviesStore.addMovie(movie);
            sendJson(exchange, 201, addedMovie);
        } catch (JsonSyntaxException e) {
            sendError(exchange, 400, "Некорректный JSON");
        }
    }

    private void handleDeleteMovie(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String idStr = path.substring(path.lastIndexOf("/") + 1);

        try {
            int id = Integer.parseInt(idStr);
            boolean deleted = moviesStore.deleteMovie(id);

            if (deleted) {
                sendNoContent(exchange);
            } else {
                sendError(exchange, 404, "Фильм с id=" + id + " не найден");
            }
        } catch (NumberFormatException e) {
            sendError(exchange, 400, "Некорректный ID фильма");
        }
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder requestBody = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                requestBody.append(line);
            }
            return requestBody.toString();
        }
    }
}
