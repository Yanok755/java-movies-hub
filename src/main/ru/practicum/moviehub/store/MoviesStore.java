package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MoviesStore {
    private final List<Movie> movies = new CopyOnWriteArrayList<>();

    public List<Movie> getAllMovies() {
        return new ArrayList<>(movies); // Возвращаем копию для безопасности
    }

    public void addMovie(Movie movie) {
        movies.add(movie);
    }

    public void clear() {
        movies.clear();
    }

    public boolean containsMovie(Movie newMovie) {
        return movies.stream().anyMatch(movie ->
            movie.getTitle().equals(newMovie.getTitle()) &&
            movie.getYear() == newMovie.getYear() &&
            movie.getDuration() == newMovie.getDuration()
        );
    }

    public int getMovieCount() {
        return movies.size();
    }
}
