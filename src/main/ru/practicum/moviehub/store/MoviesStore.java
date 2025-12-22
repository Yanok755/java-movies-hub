package ru.practicum.moviehub.store;

import java.util.ArrayList;
import java.util.List;

public class MoviesStore {
    // Простая заглушка для хранения фильмов
    private final List<Object> movies = new ArrayList<>();
    
    public List<Object> getAllMovies() {
        return new ArrayList<>(movies); // Возвращаем копию
    }
    
    public void clear() {
        movies.clear();
    }
}
