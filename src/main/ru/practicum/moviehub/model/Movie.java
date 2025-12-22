package ru.practicum.moviehub.model;

public class Movie {
    private final String title;
    private final int year;
    private final int duration;

    public Movie(String title, int year, int duration) {
        this.title = title;
        this.year = year;
        this.duration = duration;
    }

    // Getters
    public String getTitle() { return title; }
    public int getYear() { return year; }
    public int getDuration() { return duration; }

    @Override
    public String toString() {
        return "Movie{" +
                "title='" + title + '\'' +
                ", year=" + year +
                ", duration=" + duration +
                '}';
    }
}
