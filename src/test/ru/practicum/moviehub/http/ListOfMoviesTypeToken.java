package ru.practicum.moviehub.http;

import com.google.gson.reflect.TypeToken;
import ru.practicum.moviehub.model.Movie;

import java.lang.reflect.Type;
import java.util.List;

public class ListOfMoviesTypeToken extends TypeToken<List<Movie>>
{

    public static Type getType()
    {
        return new ListOfMoviesTypeToken().getType();
    }

    public static TypeToken<List<Movie>> getTypeToken()
    {
        return new ListOfMoviesTypeToken();
    }
}
