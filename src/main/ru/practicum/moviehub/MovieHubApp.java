package ru.practicum.moviehub;

import ru.practicum.moviehub.http.MoviesServer;

import java.io.IOException;

public class MovieHubApp {
    public static void main(String[] args) throws IOException {
        final MoviesServer server = new MoviesServer();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();
    }
}
