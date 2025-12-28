package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {
    private final HttpServer server;
    private final MoviesStore moviesStore;

    public MoviesServer() throws IOException {
        this.moviesStore = new MoviesStore();
        this.server = HttpServer.create(new InetSocketAddress(8080), 0);

        MoviesHandler moviesHandler = new MoviesHandler(moviesStore);
        server.createContext("/movies", moviesHandler);

        server.setExecutor(null);
    }

    public void start() {
        server.start();
        System.out.println("Сервер запущен");
    }

    public void stop() {
        server.stop(0);
        System.out.println("Сервер остановлен");
    }

    public MoviesStore getMoviesStore() {
        return moviesStore;
    }
}
