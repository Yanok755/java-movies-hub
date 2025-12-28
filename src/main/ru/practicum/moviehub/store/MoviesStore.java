package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MoviesStore {
    private final Map<Integer, Movie> movies = new HashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    public List<Movie> getAllMovie() {
        return new ArrayList<>(movies.values());
    }

    public Movie getMovieById(int id) {
        return movies.get(id);
    }

    public Movie addMovie(Movie movie) {
        int id = nextId.getAndIncrement();
        Movie newMovie = new Movie(id, movie.getName(), movie.getDescription(), movie.getDuration());

        movies.put(id, newMovie);

        return newMovie;
    }

    public boolean deleteMovie(int id) {
        return movies.remove(id) != null;
    }

    public void clear() {
        movies.clear();
        nextId.set(1);
    }

    public int size() {
        return movies.size();
    }
}
