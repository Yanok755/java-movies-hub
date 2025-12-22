package ru.practicum.moviehub.model;

public class Movie {
    // Поля можно будет добавить позже
    private String title;
    private int year;
    private int duration;
    
    // Геттеры и сеттеры
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
}
