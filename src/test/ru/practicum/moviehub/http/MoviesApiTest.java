package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;
    private static final Gson gson = new Gson();
    private static final Type MOVIE_LIST_TYPE = new TypeToken<List<Movie>>() {}.getType();

    @BeforeAll
    static void beforeAll() throws IOException {
        server = new MoviesServer();
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @BeforeEach
    void setUp() {
        server.getMoviesStore().clear();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void addMovie_validData_returnsCreatedMovie() throws Exception {
        String movieJson = String.join("\n",
                "{",
                "    \"name\": \"Тестовый фильм\",",
                "    \"description\": \"Для тестирования\",",
                "    \"duration\": 90",
                "}"
        );

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(movieJson))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, resp.statusCode(), "POST /movies должен вернуть 201 при успешном создании");

        Movie createdMovie = gson.fromJson(resp.body(), Movie.class);
        assertNotNull(createdMovie);
        assertEquals("Тестовый фильм", createdMovie.getName());
        assertEquals("Для тестирования", createdMovie.getDescription());
        assertEquals(90, createdMovie.getDuration());
        assertTrue(createdMovie.getId() > 0);
    }

    @Test
    void getMovieById_existingMovie_returnsMovie() throws Exception {

        String movieJson = String.join("\n",
                "{",
                "    \"name\": \"Тестовый фильм\",",
                "    \"description\": \"Для тестирования\",",
                "    \"duration\": 90",
                "}"
        );

        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(movieJson))
                .build();

        HttpResponse<String> postResp = client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        Movie addedMovie = gson.fromJson(postResp.body(), Movie.class);

        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + addedMovie.getId()))
                .GET()
                .build();

        HttpResponse<String> getResp = client.send(getReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, getResp.statusCode());

        Movie retrievedMovie = gson.fromJson(getResp.body(), Movie.class);
        assertEquals(addedMovie.getId(), retrievedMovie.getId());
        assertEquals(addedMovie.getName(), retrievedMovie.getName());
    }

    @Test
    void getMovieById_nonExistingMovie_returnsNotFound() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/999"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode());
    }

    @Test
    void deleteMovie_existingMovie_returnsNoContent() throws Exception {
        String movieJson = String.join("\n",
                "{",
                "    \"name\": \"Фильм для удаления\",",
                "    \"description\": \"Будет удален\",",
                "    \"duration\": 90",
                "}"
        );

        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(movieJson))
                .build();

        HttpResponse<String> postResp = client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        Movie addedMovie = gson.fromJson(postResp.body(), Movie.class);

        HttpRequest deleteReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + addedMovie.getId()))
                .DELETE()
                .build();

        HttpResponse<String> deleteResp = client.send(deleteReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(204, deleteResp.statusCode());
    }

    @Test
    void deleteMovie_nonExistingMovie_returnsNotFound() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/999"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode());
    }
}
