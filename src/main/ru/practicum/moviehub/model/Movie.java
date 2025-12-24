package ru.practicum.moviehub.model;

public class Movie {
    private String title;
    private int year;
    private int duration;

    // Конструктор по умолчанию (нужен для Gson)
    public Movie() {}

    public Movie(String title, int year, int duration) {
        this.title = title;
        this.year = year;
        this.duration = duration;
    }

    // Геттеры и сеттеры
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
}
