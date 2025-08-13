package ru.yandex.practicum.filmorate;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class FilmTest {

    @Autowired
    private Validator validator;

    @Autowired
    private FilmController filmController;

    private Film getValidFilm() {
        Film film = new Film();
        film.setId(1L);
        film.setName("Valid Film");
        film.setDescription("This is a valid film description.");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        return film;
    }

    @BeforeEach
    void setUp() {
        if (validator == null || filmController == null) {
            throw new IllegalStateException("Validator or FilmController is not injected. Check Spring context configuration.");
        }
    }

    @Test
    void shouldFailWhenNameIsBlank() {
        Film film = getValidFilm();
        film.setName("");
        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertThat(violations)
                .anyMatch(v -> v.getMessage().contains("Название не может быть пустым"));
    }

    @Test
    void shouldFailWhenDescriptionTooLong() {
        Film film = getValidFilm();
        film.setDescription("A".repeat(201)); // лимит 200 символов
        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertThat(violations)
                .anyMatch(v -> v.getMessage().contains("Описание не должно превышать 200 символов"));
    }

    @Test
    void shouldFailWhenReleaseDateBeforeCinemaBirth() {
        Film film = getValidFilm();
        film.setReleaseDate(LocalDate.of(1800, 1, 1)); // раньше 1895 года
        // Проверка через контроллер, так как валидация даты не в Validator
        assertThrows(ConditionsNotMetException.class, () -> filmController.create(film),
                "Ожидается исключение ConditionsNotMetException для даты раньше 1895 года");
    }

    @Test
    void shouldFailWhenDurationIsNegative() {
        Film film = getValidFilm();
        film.setDuration(-10);
        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertThat(violations)
                .anyMatch(v -> v.getMessage().contains("Продолжительность фильма должна быть положительным числом"));
    }

    @Test
    void shouldPassWhenFilmIsValid() {
        Film film = getValidFilm();
        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertThat(violations).isEmpty();
    }
}