package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {
    private final HttpServer server;

    public MoviesServer(MoviesStore moviesStore, int port) {
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
            this.server.createContext("/movies", new MoviesHandler(moviesStore));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create HTTP server on port " + port, e);
        }
    }

    public void start() {
        server.start();
        System.out.println("MovieHub server started on http://localhost:" + server.getAddress().getPort());
    }

    public void stop() {
        server.stop(0);
        System.out.println("MovieHub server stopped");
    }
}
