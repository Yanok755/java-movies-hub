package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore moviesStore;

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if ("/movies".equals(path)) {
                if ("GET".equalsIgnoreCase(method)) {
                    handleGetMovies(exchange);
                } else if ("POST".equalsIgnoreCase(method)) {
                    handlePostMovie(exchange);
                } else {
                    sendError(exchange, 405, "Method " + method + " not allowed. Supported methods: GET, POST");
                }
            } else {
                sendError(exchange, 404, "Endpoint not found: " + path);
            }
        } catch (IllegalArgumentException e) {
            // Ошибки валидации
            sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            // Внутренние ошибки сервера
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    private void handleGetMovies(HttpExchange exchange) throws IOException {
        sendJson(exchange, 200, moviesStore.getAllMovies());
    }

    private void handlePostMovie(HttpExchange exchange) throws IOException {
        // Проверяем Content-Type
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.contains("application/json")) {
            sendError(exchange, 400, "Content-Type must be application/json");
            return;
        }

        // Парсим тело запроса
        Movie movie = parseJsonBody(exchange, Movie.class);

        // Валидация полей фильма
        validateMovie(movie);

        // Проверяем на дубликаты
        if (moviesStore.containsMovie(movie)) {
            sendError(exchange, 409, "Movie already exists: " + movie.getTitle() + " (" + movie.getYear() + ")");
            return;
        }

        // Добавляем фильм
        moviesStore.addMovie(movie);

        // Возвращаем успешный ответ
        sendJson(exchange, 201, movie);
    }

    private void validateMovie(Movie movie) {
        if (movie == null) {
            throw new IllegalArgumentException("Movie cannot be null");
        }

        if (movie.getTitle() == null || movie.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }

        if (movie.getYear() <= 1888) { // Первый фильм был в 1888
            throw new IllegalArgumentException("Year must be after 1888");
        }

        if (movie.getYear() > java.time.Year.now().getValue() + 1) { // Можно добавить фильмы на следующий год
            throw new IllegalArgumentException("Year cannot be in the distant future");
        }

        if (movie.getDuration() <= 0) {
            throw new IllegalArgumentException("Duration must be positive");
        }

        if (movie.getDuration() > 1000) { // Максимальная продолжительность 1000 минут
            throw new IllegalArgumentException("Duration is too long (max 1000 minutes)");
        }
    }
}
