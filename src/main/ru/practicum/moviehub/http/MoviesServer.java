package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {
    private final HttpServer server;
    private final int port;

    public MoviesServer(MoviesStore moviesStore, int port) {
        this.port = port;
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
            this.server.createContext("/", new MoviesHandler(moviesStore));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create HTTP server on port " + port, e);
        }
    }

    public void start() {
        server.start();
        System.out.println("MovieHub server started on http://localhost:" + port);
        System.out.println("Endpoints:");
        System.out.println("  GET    /movies           - get all movies");
        System.out.println("  POST   /movies           - add a new movie");
        System.out.println("  GET    /movies/{id}      - get movie by ID");
        System.out.println("  DELETE /movies/{id}      - delete movie by ID");
    }

    public void stop() {
        server.stop(0);
        System.out.println("MovieHub server stopped");
    }
}
