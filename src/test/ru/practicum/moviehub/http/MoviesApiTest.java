package ru.practicum.moviehub;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;
import ru.practicum.moviehub.http.MoviesServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MoviesApiTest {
    private static final String BASE_URL = "http://localhost:8080";
    private static final String MOVIES_ENDPOINT = BASE_URL + "/movies";
    
    private static MoviesServer server;
    private static HttpClient httpClient;
    private static MoviesStore moviesStore;

    @BeforeAll
    static void setUpAll() throws InterruptedException {
        moviesStore = new MoviesStore();
        server = new MoviesServer(moviesStore, 8080);
        server.start();

        // Даем серверу время на запуск
        TimeUnit.MILLISECONDS.sleep(500);

        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @AfterAll
    static void tearDownAll() {
        if (server != null) {
            server.stop();
        }
    }

    @BeforeEach
    void setUp() {
        // Очищаем хранилище перед каждым тестом
        moviesStore.clear();
    }

    @Test
    @Order(1)
    @DisplayName("GET /movies при пустом хранилище возвращает пустой массив")
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        // Arrange
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MOVIES_ENDPOINT))
                .GET()
                .build();

        // Act
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // Assert
        assertEquals(200, response.statusCode(), "Статус код должен быть 200 OK");

        String contentType = response.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType, 
                "Заголовок Content-Type должен быть правильным");

        String body = response.body().trim();
        assertEquals("[]", body, "Тело ответа должно быть пустым массивом");
    }

    @Test
    @Order(2)
    @DisplayName("POST /movies с валидными данными возвращает 201 Created")
    void postMovie_whenValidData_returns201Created() throws Exception {
        // Arrange
        String movieJson = """
            {
                "title": "Inception",
                "year": 2010,
                "duration": 148
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MOVIES_ENDPOINT))
                .POST(HttpRequest.BodyPublishers.ofString(movieJson))
                .header("Content-Type", "application/json")
                .build();

        // Act
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // Assert
        assertEquals(201, response.statusCode(), "Статус код должен быть 201 Created");

        String contentType = response.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType,
                "Заголовок Content-Type должен быть правильным");
    }

    @Test
    @Order(3)
    @DisplayName("GET /movies после добавления фильма возвращает фильм в массиве")
    void getMovies_afterAddingMovie_returnsMovieInArray() throws Exception {
        // Arrange - добавляем фильм
        String movieJson = """
            {
                "title": "The Matrix",
                "year": 1999,
                "duration": 136
            }
            """;

        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create(MOVIES_ENDPOINT))
                .POST(HttpRequest.BodyPublishers.ofString(movieJson))
                .header("Content-Type", "application/json")
                .build();

        httpClient.send(postRequest, HttpResponse.BodyHandlers.discarding());

        // Act - получаем список фильмов
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(MOVIES_ENDPOINT))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // Assert
        assertEquals(200, response.statusCode());

        JsonArray moviesArray = JsonParser.parseString(response.body()).getAsJsonArray();
        assertEquals(1, moviesArray.size(), "Массив должен содержать 1 фильм");

        JsonObject movie = moviesArray.get(0).getAsJsonObject();
        assertEquals("The Matrix", movie.get("title").getAsString());
        assertEquals(1999, movie.get("year").getAsInt());
        assertEquals(136, movie.get("duration").getAsInt());
    }

    @Test
    @Order(4)
    @DisplayName("POST /movies с невалидным JSON возвращает 400 Bad Request")
    void postMovie_whenInvalidJson_returns400BadRequest() throws Exception {
        // Arrange
        String invalidJson = """
            {
                "title": "Inception",
                "year": "not-a-number",
                "duration": 148
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MOVIES_ENDPOINT))
                .POST(HttpRequest.BodyPublishers.ofString(invalidJson))
                .header("Content-Type", "application/json")
                .build();

        // Act
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // Assert
        assertEquals(400, response.statusCode(), "Статус код должен быть 400 Bad Request");

        String contentType = response.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType);

        // Проверяем, что в теле есть описание ошибки
        String body = response.body();
        assertTrue(body.contains("error") || body.contains("message"), 
                "Тело ответа должно содержать описание ошибки");
    }

    @Test
    @Order(5)
    @DisplayName("POST /movies без обязательных полей возвращает 400 Bad Request")
    void postMovie_whenMissingRequiredFields_returns400BadRequest() throws Exception {
        // Arrange
        String incompleteJson = """
            {
                "title": "Inception"
                // year и duration отсутствуют
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MOVIES_ENDPOINT))
                .POST(HttpRequest.BodyPublishers.ofString(incompleteJson))
                .header("Content-Type", "application/json")
                .build();

        // Act
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // Assert
        assertEquals(400, response.statusCode(), "Статус код должен быть 400 Bad Request");
    }

    @Test
    @Order(6)
    @DisplayName("POST /movies с некорректными значениями полей возвращает 400 Bad Request")
    void postMovie_whenInvalidFieldValues_returns400BadRequest() throws Exception {
        // Arrange - год в будущем, отрицательная продолжительность
        String invalidDataJson = """
            {
                "title": "Future Movie",
                "year": 3000,
                "duration": -120
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MOVIES_ENDPOINT))
                .POST(HttpRequest.BodyPublishers.ofString(invalidDataJson))
                .header("Content-Type", "application/json")
                .build();

        // Act
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // Assert
        assertEquals(400, response.statusCode(), "Статус код должен быть 400 Bad Request");
    }

    @Test
    @Order(7)
    @DisplayName("Неверный HTTP метод возвращает 405 Method Not Allowed")
    void unsupportedHttpMethod_returns405MethodNotAllowed() throws Exception {
        // Arrange
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MOVIES_ENDPOINT))
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();

        // Act
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // Assert
        assertEquals(405, response.statusCode(), "Статус код должен быть 405 Method Not Allowed");
    }

    @Test
    @Order(8)
    @DisplayName("POST /movies с пустым телом возвращает 400 Bad Request")
    void postMovie_whenEmptyBody_returns400BadRequest() throws Exception {
        // Arrange
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MOVIES_ENDPOINT))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Content-Type", "application/json")
                .build();

        // Act
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // Assert
        assertEquals(400, response.statusCode(), "Статус код должен быть 400 Bad Request");
    }

    @Test
    @Order(9)
    @DisplayName("POST /movies с некорректным Content-Type возвращает 400 Bad Request")
    void postMovie_whenWrongContentType_returns400BadRequest() throws Exception {
        // Arrange
        String movieJson = """
            {
                "title": "Inception",
                "year": 2010,
                "duration": 148
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MOVIES_ENDPOINT))
                .POST(HttpRequest.BodyPublishers.ofString(movieJson))
                .header("Content-Type", "text/plain") // Неправильный Content-Type
                .build();

        // Act
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // Assert
        assertEquals(400, response.statusCode(), "Статус код должен быть 400 Bad Request");
    }

    @Test
    @Order(10)
    @DisplayName("GET /movies возвращает корректный JSON при нескольких фильмах")
    void getMovies_withMultipleMovies_returnsCorrectJson() throws Exception {
        // Arrange - добавляем несколько фильмов
        String[] movies = {
            """
            {
                "title": "The Shawshank Redemption",
                "year": 1994,
                "duration": 142
            }
            """,
            """
            {
                "title": "The Godfather",
                "year": 1972,
                "duration": 175
            }
            """,
            """
            {
                "title": "The Dark Knight",
                "year": 2008,
                "duration": 152
            }
            """
        };

        for (String movieJson : movies) {
            HttpRequest postRequest = HttpRequest.newBuilder()
                    .uri(URI.create(MOVIES_ENDPOINT))
                    .POST(HttpRequest.BodyPublishers.ofString(movieJson))
                    .header("Content-Type", "application/json")
                    .build();
            httpClient.send(postRequest, HttpResponse.BodyHandlers.discarding());
        }

        // Act
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(MOVIES_ENDPOINT))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // Assert
        assertEquals(200, response.statusCode());

        JsonArray moviesArray = JsonParser.parseString(response.body()).getAsJsonArray();
        assertEquals(3, moviesArray.size(), "Массив должен содержать 3 фильма");

        // Проверяем, что все фильмы присутствуют
        String[] expectedTitles = {"The Shawshank Redemption", "The Godfather", "The Dark Knight"};
        for (String expectedTitle : expectedTitles) {
            boolean found = false;
            for (int i = 0; i < moviesArray.size(); i++) {
                if (moviesArray.get(i).getAsJsonObject().get("title").getAsString().equals(expectedTitle)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Фильм '" + expectedTitle + "' должен присутствовать в ответе");
        }
    }

    @Test
    @Order(11)
    @DisplayName("POST /movies с дублирующимся фильмом возвращает 409 Conflict")
    void postMovie_whenDuplicateMovie_returns409Conflict() throws Exception {
        // Arrange - добавляем фильм первый раз
        String movieJson = """
            {
                "title": "Pulp Fiction",
                "year": 1994,
                "duration": 154
            }
            """;

        HttpRequest firstRequest = HttpRequest.newBuilder()
                .uri(URI.create(MOVIES_ENDPOINT))
                .POST(HttpRequest.BodyPublishers.ofString(movieJson))
                .header("Content-Type", "application/json")
                .build();

        httpClient.send(firstRequest, HttpResponse.BodyHandlers.discarding());

        // Act - пытаемся добавить тот же фильм снова
        HttpRequest secondRequest = HttpRequest.newBuilder()
                .uri(URI.create(MOVIES_ENDPOINT))
                .POST(HttpRequest.BodyPublishers.ofString(movieJson))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(secondRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // Assert
        assertEquals(409, response.statusCode(), "Статус код должен быть 409 Conflict");
    }

    @Test
    @Order(12)
    @DisplayName("Несуществующий эндпоинт возвращает 404 Not Found")
    void nonexistentEndpoint_returns404NotFound() throws Exception {
        // Arrange
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/nonexistent"))
                .GET()
                .build();

        // Act
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // Assert
        assertEquals(404, response.statusCode(), "Статус код должен быть 404 Not Found");
    }

    @Test
    @Order(13)
    @DisplayName("GET /movies с заголовком Accept возвращает JSON")
    void getMovies_withAcceptHeader_returnsJson() throws Exception {
        // Arrange
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MOVIES_ENDPOINT))
                .GET()
                .header("Accept", "application/json")
                .build();

        // Act
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // Assert
        assertEquals(200, response.statusCode());
        assertEquals("application/json; charset=UTF-8", 
                response.headers().firstValue("Content-Type").orElse(""));
    }

    @Test
    @Order(14)
    @DisplayName("POST /movies с максимально допустимыми значениями работает корректно")
    void postMovie_withBoundaryValues_returns201() throws Exception {
        // Arrange
        String movieJson = """
            {
                "title": "A".repeat(200),
                "year": 2024,
                "duration": 999
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MOVIES_ENDPOINT))
                .POST(HttpRequest.BodyPublishers.ofString(movieJson))
                .header("Content-Type", "application/json")
                .build();

        // Act
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // Assert
        assertEquals(201, response.statusCode(), "Статус код должен быть 201 Created");
    }

    @Test
    @Order(15)
    @DisplayName("GET /movies возвращает фильмы в порядке добавления")
    void getMovies_returnsMoviesInInsertionOrder() throws Exception {
        // Arrange - добавляем фильмы в определенном порядке
        String[][] moviesData = {
            {"First Movie", "2001", "100"},
            {"Second Movie", "2002", "110"},
            {"Third Movie", "2003", "120"}
        };

        for (String[] movieData : moviesData) {
            String movieJson = String.format("""
                {
                    "title": "%s",
                    "year": %s,
                    "duration": %s
                }
                """, movieData[0], movieData[1], movieData[2]);

            HttpRequest postRequest = HttpRequest.newBuilder()
                    .uri(URI.create(MOVIES_ENDPOINT))
                    .POST(HttpRequest.BodyPublishers.ofString(movieJson))
                    .header("Content-Type", "application/json")
                    .build();
            httpClient.send(postRequest, HttpResponse.BodyHandlers.discarding());
        }

        // Act
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(MOVIES_ENDPOINT))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // Assert
        JsonArray moviesArray = JsonParser.parseString(response.body()).getAsJsonArray();

        for (int i = 0; i < moviesData.length; i++) {
            JsonObject movie = moviesArray.get(i).getAsJsonObject();
            assertEquals(moviesData[i][0], movie.get("title").getAsString(),
                    "Фильмы должны быть в порядке добавления");
        }
    }
}
