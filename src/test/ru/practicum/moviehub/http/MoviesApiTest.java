package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import ru.practicum.moviehub.MoviesServer;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("MovieHub API Tests")
public class MoviesApiTest {
    private static final String BASE_URL = "http://localhost:8080";
    private static final String MOVIES_ENDPOINT = BASE_URL + "/movies";

    private static MoviesServer server;
    private static HttpClient httpClient;
    private static MoviesStore moviesStore;
    private static final Gson gson = new GsonBuilder().create();

    @BeforeAll
    static void setUpAll() throws InterruptedException {
        // Создаем хранилище и сервер
        moviesStore = new MoviesStore();
        server = new MoviesServer(moviesStore, 8080);
        server.start();

        // Даем серверу время на запуск
        TimeUnit.MILLISECONDS.sleep(500);

        // Настраиваем HTTP клиент
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        System.out.println("Test setup complete. Server is running on " + BASE_URL);
    }

    @AfterAll
    static void tearDownAll() {
        if (server != null) {
            server.stop();
        }
        System.out.println("Test cleanup complete. Server stopped.");
    }

    @BeforeEach
    void setUp() {
        // Очищаем хранилище перед каждым тестом
        moviesStore.clear();
        System.out.println("Cleared movies store before test");
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

        // Альтернативная проверка с использованием TypeToken
        TypeToken<List<Movie>> typeToken = new TypeToken<>() {};
        List<Movie> movies = gson.fromJson(body, typeToken.getType());
        assertTrue(movies.isEmpty(), "Список фильмов должен быть пустым");
    }

    @Test
    @Order(2)
    @DisplayName("POST /movies с валидными данными возвращает 201 Created и фильм")
    void postMovie_whenValidData_returns201Created() throws Exception {
        // Arrange
        String movieJson = "{\"title\": \"Inception\", \"year\": 2010, \"duration\": 148}";

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

        // Проверяем, что в ответе вернулся добавленный фильм
        Movie responseMovie = gson.fromJson(response.body(), Movie.class);
        assertEquals("Inception", responseMovie.getTitle());
        assertEquals(2010, responseMovie.getYear());
        assertEquals(148, responseMovie.getDuration());

        // Проверяем, что фильм действительно добавлен в хранилище
        assertEquals(1, moviesStore.getMovieCount(), "В хранилище должен быть 1 фильм");
    }

    @Test
    @Order(3)
    @DisplayName("GET /movies после добавления фильма возвращает фильм в массиве")
    void getMovies_afterAddingMovie_returnsMovieInArray() throws Exception {
        // Arrange - добавляем фильм
        String movieJson = "{\"title\": \"The Matrix\", \"year\": 1999, \"duration\": 136}";

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
        String invalidJson = "{ invalid json }";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MOVIES_ENDPOINT))
                .POST(HttpRequest.BodyPublishers.ofString(invalidJson))
                .header("Content-Type", "application/json")
                .build();

        // Act
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // Assert
        assertEquals(400, response.statusCode(), "Статус код должен быть 400 Bad Request");

        String body = response.body();
        assertTrue(body.contains("error"), "Тело ответа должно содержать поле 'error'");
    }

    @Test
    @Order(5)
    @DisplayName("POST /movies без обязательных полей возвращает 400 Bad Request")
    void postMovie_whenMissingRequiredFields_returns400BadRequest() throws Exception {
        // Arrange
        String incompleteJson = "{\"title\": \"Inception\"}";

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
        String invalidDataJson = "{\"title\": \"Future Movie\", \"year\": 3000, \"duration\": -120}";

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
    @DisplayName("POST /movies с пустым названием возвращает 400 Bad Request")
    void postMovie_whenEmptyTitle_returns400BadRequest() throws Exception {
        // Arrange
        String emptyTitleJson = "{\"title\": \"\", \"year\": 2020, \"duration\": 120}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MOVIES_ENDPOINT))
                .POST(HttpRequest.BodyPublishers.ofString(emptyTitleJson))
                .header("Content-Type", "application/json")
                .build();

        // Act
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // Assert
        assertEquals(400, response.statusCode(), "Статус код должен быть 400 Bad Request");
    }

    @Test
    @Order(8)
    @DisplayName("POST /movies с дублирующимся фильмом возвращает 409 Conflict")
    void postMovie_whenDuplicateMovie_returns409Conflict() throws Exception {
        // Arrange - добавляем фильм первый раз
        String movieJson = "{\"title\": \"Pulp Fiction\", \"year\": 1994, \"duration\": 154}";

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

        String body = response.body();
        assertTrue(body.contains("already exists"), "Сообщение об ошибке должно указывать на существующий фильм");
    }

    @Test
    @Order(9)
    @DisplayName("POST /movies без заголовка Content-Type возвращает 400 Bad Request")
    void postMovie_withoutContentType_returns400BadRequest() throws Exception {
        // Arrange
        String movieJson = "{\"title\": \"Some Movie\", \"year\": 2020, \"duration\": 120}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MOVIES_ENDPOINT))
                .POST(HttpRequest.BodyPublishers.ofString(movieJson))
                // Не указываем Content-Type
                .build();

        // Act
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // Assert
        assertEquals(400, response.statusCode(), "Статус код должен быть 400 Bad Request");
    }

    @Test
    @Order(10)
    @DisplayName("POST /movies с неправильным Content-Type возвращает 400 Bad Request")
    void postMovie_whenWrongContentType_returns400BadRequest() throws Exception {
        // Arrange
        String movieJson = "{\"title\": \"Some Movie\", \"year\": 2020, \"duration\": 120}";

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
    @Order(11)
    @DisplayName("GET /movies возвращает корректный JSON при нескольких фильмах")
    void getMovies_withMultipleMovies_returnsCorrectJson() throws Exception {
        // Arrange - добавляем несколько фильмов
        String[][] moviesData = {
            {"The Shawshank Redemption", "1994", "142"},
            {"The Godfather", "1972", "175"},
            {"The Dark Knight", "2008", "152"}
        };

        for (String[] movieData : moviesData) {
            String movieJson = String.format(
                "{\"title\": \"%s\", \"year\": %s, \"duration\": %s}",
                movieData[0], movieData[1], movieData[2]
            );

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

        // Используем TypeToken для десериализации
        TypeToken<List<Movie>> typeToken = new TypeToken<>() {};
        List<Movie> movieList = gson.fromJson(response.body(), typeToken.getType());

        assertEquals(3, movieList.size(), "Массив должен содержать 3 фильма");

        // Проверяем, что все фильмы присутствуют
        String[] expectedTitles = {"The Shawshank Redemption", "The Godfather", "The Dark Knight"};
        for (String expectedTitle : expectedTitles) {
            boolean found = movieList.stream()
                    .anyMatch(movie -> movie.getTitle().equals(expectedTitle));
            assertTrue(found, "Фильм '" + expectedTitle + "' должен присутствовать в ответе");
        }
    }

    @Test
    @Order(12)
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
    @Order(13)
    @DisplayName("GET /movies возвращает фильмы в порядке добавления")
    void getMovies_returnsMoviesInInsertionOrder() throws Exception {
        // Arrange - добавляем фильмы в определенном порядке
        String[][] moviesData = {
            {"First Movie", "2001", "100"},
            {"Second Movie", "2002", "110"},
            {"Third Movie", "2003", "120"}
        };

        for (String[] movieData : moviesData) {
            String movieJson = String.format(
                "{\"title\": \"%s\", \"year\": %s, \"duration\": %s}",
                movieData[0], movieData[1], movieData[2]
            );

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
        TypeToken<List<Movie>> typeToken = new TypeToken<>() {};
        List<Movie> movieList = gson.fromJson(response.body(), typeToken.getType());

        for (int i = 0; i < moviesData.length; i++) {
            Movie movie = movieList.get(i);
            assertEquals(moviesData[i][0], movie.getTitle(),
                    "Фильмы должны быть в порядке добавления, позиция " + i);
        }
    }

    @Test
    @Order(14)
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

        String body = response.body();
        assertTrue(body.contains("error"), "Тело ответа должно содержать описание ошибки");
        assertTrue(body.contains("not found"), "Сообщение об ошибке должно указывать на отсутствие эндпоинта");
    }

    @Test
    @Order(15)
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
    @Order(16)
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
    @Order(17)
    @DisplayName("Проверка на очень старый фильм (до 1888 года)")
    void postMovie_withVeryOldYear_returns400BadRequest() throws Exception {
        // Arrange
        String oldMovieJson = "{\"title\": \"Very Old Movie\", \"year\": 1800, \"duration\": 60}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MOVIES_ENDPOINT))
                .POST(HttpRequest.BodyPublishers.ofString(oldMovieJson))
                .header("Content-Type", "application/json")
                .build();

        // Act
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // Assert
        assertEquals(400, response.statusCode(), "Статус код должен быть 400 Bad Request");
    }

    @Test
    @Order(18)
    @DisplayName("Проверка на слишком длинный фильм")
    void postMovie_withTooLongDuration_returns400BadRequest() throws Exception {
        // Arrange
        String longMovieJson = "{\"title\": \"Very Long Movie\", \"year\": 2020, \"duration\": 2000}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MOVIES_ENDPOINT))
                .POST(HttpRequest.BodyPublishers.ofString(longMovieJson))
                .header("Content-Type", "application/json")
                .build();

        // Act
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // Assert
        assertEquals(400, response.statusCode(), "Статус код должен быть 400 Bad Request");
    }

    @Test
    @Order(19)
    @DisplayName("Интеграционный тест: полный цикл добавления и получения фильмов")
    void integrationTest_fullCycle() throws Exception {
        // Arrange
        String[] moviesToAdd = {
            "{\"title\": \"Movie 1\", \"year\": 2001, \"duration\": 100}",
            "{\"title\": \"Movie 2\", \"year\": 2002, \"duration\": 110}",
            "{\"title\": \"Movie 3\", \"year\": 2003, \"duration\": 120}"
        };

        // Act & Assert - добавляем фильмы
        for (int i = 0; i < moviesToAdd.length; i++) {
            HttpRequest postRequest = HttpRequest.newBuilder()
                    .uri(URI.create(MOVIES_ENDPOINT))
                    .POST(HttpRequest.BodyPublishers.ofString(moviesToAdd[i]))
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<String> postResponse = httpClient.send(postRequest, 
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            assertEquals(201, postResponse.statusCode(), "Добавление фильма " + (i + 1) + " должно вернуть 201");
        }

        // Проверяем, что все фильмы добавлены
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(MOVIES_ENDPOINT))
                .GET()
                .build();

        HttpResponse<String> getResponse = httpClient.send(getRequest, 
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, getResponse.statusCode());

        TypeToken<List<Movie>> typeToken = new TypeToken<>() {};
        List<Movie> movies = gson.fromJson(getResponse.body(), typeToken.getType());
        assertEquals(3, movies.size(), "Должно быть 3 фильма в хранилище");
    }

    @Test
    @Order(20)
    @DisplayName("Проверка изоляции тестов: каждый тест начинается с чистого хранилища")
    void testIsolation_checkEmptyStoreAfterPreviousTests() {
        // Этот тест должен выполняться последним, чтобы проверить, что @BeforeEach работает правильно
        assertEquals(0, moviesStore.getMovieCount(), 
                "Хранилище должно быть пустым в начале каждого теста");
    }
}
