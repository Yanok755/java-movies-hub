package ru.practicum.moviehub.model;

public class Movie {
    private int id;
    private String name;
    private String description;
    private int duration;

    public Movie() {
    }

    public Movie(String name, String description, int duration) {
        this.name = name;
        this.description = description;
        this.duration = duration;
    }

    public Movie(int id, String name, String description, int duration) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.duration = duration;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    @Override
    public String toString() {
        return "Movie{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", duration=" + duration +
                '}';
    }
}
