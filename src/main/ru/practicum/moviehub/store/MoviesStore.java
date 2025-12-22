package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MoviesStore {
    private final List<Movie> movies = new ArrayList<>();
    private final AtomicInteger idCounter = new AtomicInteger(1);

    public List<Movie> getAllMovies() {
        return new ArrayList<>(movies); // Возвращаем копию для безопасности
    }

    public Movie addMovie(Movie movie) {
        movie.setId(idCounter.getAndIncrement());
        movies.add(movie);
        return movie;
    }

    public Movie getMovieById(int id) {
        return movies.stream()
                .filter(m -> m.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public boolean deleteMovie(int id) {
        return movies.removeIf(m -> m.getId() == id);
    }

    public void clear() {
        movies.clear();
        idCounter.set(1);
    }

    public int size() {
        return movies.size();
    }
}
