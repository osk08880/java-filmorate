package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/films")
@Slf4j
public class FilmController {
    private final Map<Long, Film> films = new HashMap<>();
    private final Validator validator;

    @Autowired
    public FilmController(Validator validator) {
        if (validator == null) {
            throw new IllegalStateException("Validator не внедрён. Проверь конфигурацию контекста Spring.");
        }
        this.validator = validator;
    }

    private void validateEntity(Film film) {
        var violations = validator.validate(film);
        if (!violations.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder();
            violations.forEach(violation -> {
                String fieldName = violation.getPropertyPath().toString();
                String errorMsg = violation.getMessage();
                errorMessage.append(fieldName).append(": ").append(errorMsg).append("; ");
                log.warn("Ошибка валидации в FilmController: поле '{}' - {}", fieldName, errorMsg);
            });
            throw new ValidationException(errorMessage.toString());
        }
    }

    @PostMapping
    public Film create(@Valid @RequestBody Film film) {
        try {
            validateEntity(film);

            LocalDate minReleaseDate = LocalDate.of(1895, 12, 28);
            if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(minReleaseDate)) {
                log.warn("Ошибка создания фильма в FilmController: дата релиза {} раньше минимальной даты {}",
                        film.getReleaseDate(), minReleaseDate);
                throw new ConditionsNotMetException("Дата релиза не может быть раньше 28 декабря 1895 года");
            }

            film.setId(getNextId());
            films.put(film.getId(), film);

            log.info("Фильм успешно создан в FilmController: id={}, name={}, releaseDate={}",
                    film.getId(), film.getName(), film.getReleaseDate());
            return film;
        } catch (ValidationException | ConditionsNotMetException e) {
            throw e;
        } catch (Exception e) {
            log.error("Неизвестная ошибка в FilmController: {}", e.getMessage(), e);
            throw new RuntimeException("Внутренняя ошибка сервера", e);
        }
    }

    @PutMapping
    public Film update(@Valid @RequestBody Film newFilm) {
        try {
            validateEntity(newFilm);

            if (newFilm.getId() == null) {
                log.warn("Ошибка обновления фильма в FilmController: не указан id");
                throw new ConditionsNotMetException("Id должен быть указан");
            }
            if (!films.containsKey(newFilm.getId())) {
                log.warn("Ошибка обновления фильма в FilmController: фильм с id={} не найден", newFilm.getId());
                throw new NotFoundException("Фильм с id " + newFilm.getId() + " не найден");
            }

            LocalDate minReleaseDate = LocalDate.of(1895, 12, 28);
            if (newFilm.getReleaseDate() != null && newFilm.getReleaseDate().isBefore(minReleaseDate)) {
                log.warn("Ошибка обновления фильма в FilmController: дата релиза {} раньше минимальной даты {}",
                        newFilm.getReleaseDate(), minReleaseDate);
                throw new ConditionsNotMetException("Дата релиза не может быть раньше 28 декабря 1895 года");
            }

            Film oldFilm = films.get(newFilm.getId());
            oldFilm.setName(newFilm.getName());
            oldFilm.setDescription(newFilm.getDescription());
            oldFilm.setReleaseDate(newFilm.getReleaseDate());
            oldFilm.setDuration(newFilm.getDuration());

            log.info("Фильм успешно обновлен в FilmController: id={}, name={}, releaseDate={}",
                    oldFilm.getId(), oldFilm.getName(), oldFilm.getReleaseDate());
            return oldFilm;
        } catch (ValidationException | ConditionsNotMetException | NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Неизвестная ошибка в FilmController: {}", e.getMessage(), e);
            throw new RuntimeException("Внутренняя ошибка сервера", e);
        }
    }

    @GetMapping
    public Collection<Film> findAll() {
        return Collections.unmodifiableCollection(new ArrayList<>(films.values()));
    }

    private synchronized long getNextId() {
        long currentMaxId = films.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }
}
