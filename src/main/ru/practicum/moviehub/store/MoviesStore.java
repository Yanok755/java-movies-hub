package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;
import java.util.ArrayList;
import java.util.List;

public class MoviesStore {
    private final List<Movie> movies = new ArrayList<>();
    
    public List<Movie> getAllMovies() {
        return new ArrayList<>(movies); // Возвращаем копию для безопасности
    }
    
    public void addMovie(Movie movie) {
        movies.add(movie);
    }
    
    public void clear() {
        movies.clear();
    }
}
