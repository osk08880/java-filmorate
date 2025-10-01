package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
public class Film {
    private Long id;
    @NotBlank(message = "Название не может быть пустым")
    private String name;
    @Size(max = 200, message = "Описание не должно превышать 200 символов")
    private String description;
    private LocalDate releaseDate;
    @Positive(message = "Продолжительность фильма должна быть положительным числом")
    private int duration;
    private Set<Long> likes = new HashSet<>();
    private Set<Genre> genres = new HashSet<>();
    private MpaRating mpa;

    public enum Genre {
        КОМЕДИЯ,
        ДРАМА,
        МУЛЬТФИЛЬМ,
        ТРИЛЛЕР,
        ДОКУМЕНТАЛЬНЫЙ,
        БОЕВИК
    }

    public enum MpaRating {
        G,      // Нет возрастных ограничений
        PG,     // Детям рекомендуется смотреть с родителями
        PG_13,  // Детям до 13 лет просмотр нежелателен
        R,      // Лицам до 17 лет просмотр только со взрослым
        NC_17   // Лицам до 18 лет просмотр запрещён
    }
}