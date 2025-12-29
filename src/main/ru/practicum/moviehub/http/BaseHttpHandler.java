package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public abstract class BaseHttpHandler implements HttpHandler {
    protected static final String CT_JSON = "application/json; charset=UTF-8";
    protected final Gson gson;

    public BaseHttpHandler(Gson gson) {
        if (gson == null) {
            throw new IllegalArgumentException("Gson не может быть null");
        }
        this.gson = gson;
    }

    protected void sendJson(HttpExchange exchange, int statusCode, Object response) throws IOException {
        String json = gson.toJson(response);
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", CT_JSON);
        exchange.sendResponseHeaders(statusCode, jsonBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(jsonBytes);
        }
    }

    protected void sendText(HttpExchange exchange, int statusCode, String text) throws IOException {
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, textBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(textBytes);
        }
    }

    protected void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        ErrorResponse errorResponse = new ErrorResponse(message);
        sendJson(exchange, statusCode, errorResponse);
    }

    protected void sendNoContent(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(204, -1);
        exchange.getResponseBody().close();
    }

    protected <T> T parseJson(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }
}
