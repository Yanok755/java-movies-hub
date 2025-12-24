package ru.practicum.moviehub;

import ru.practicum.moviehub.http.MoviesServer;
import ru.practicum.moviehub.store.MoviesStore;

public class MovieHubApp {
    public static void main(String[] args) {
        // Порт по умолчанию
        int port = 8080;

        // Можно указать порт через аргументы командной строки
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[0]);
                System.err.println("Using default port: 8080");
            }
        }

        // Создаем и запускаем сервер
        final MoviesServer server = new MoviesServer(new MoviesStore(), port);

        // Добавляем хук для корректного завершения
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down MovieHub server...");
            server.stop();
        }));

        // Запускаем сервер
        server.start();
    }
}
