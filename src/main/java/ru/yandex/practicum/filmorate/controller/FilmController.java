package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;

import java.util.*;

@RestController
@RequestMapping("/films")
@Slf4j
public class FilmController {
    private final FilmStorage filmStorage;
    private final FilmService filmService;
    private final Validator validator;

    @Autowired
    public FilmController(FilmStorage filmStorage, FilmService filmService, Validator validator) {
        this.filmStorage = filmStorage;
        this.filmService = filmService;
        this.validator = validator;
    }

    private void validateEntity(Film film) {
        log.debug("Начало валидации фильма: {}", film);
        var violations = validator.validate(film);
        if (!violations.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder();
            violations.forEach(violation -> {
                String fieldName = violation.getPropertyPath().toString();
                String errorMsg = violation.getMessage();
                errorMessage.append(fieldName).append(": ").append(errorMsg).append("; ");
                log.warn("Ошибка валидации: поле '{}' - {}", fieldName, errorMsg);
            });
            throw new ValidationException(errorMessage.toString());
        }
        log.debug("Валидация фильма успешно пройдена: {}", film);
    }

    @PostMapping
    public Film create(@Valid @RequestBody Film film) {
        log.debug("Получен запрос на создание фильма: {}", film);
        validateEntity(film);
        Film createdFilm = filmStorage.create(film);
        log.info("Фильм успешно создан: id={}", createdFilm.getId());
        return createdFilm;
    }

    @PutMapping
    public Film update(@Valid @RequestBody Film film) {
        log.debug("Получен запрос на обновление фильма: {}", film);
        validateEntity(film);
        Film updatedFilm = filmStorage.update(film);
        log.info("Фильм успешно обновлен: id={}", updatedFilm.getId());
        return updatedFilm;
    }

    @GetMapping
    public Collection<Film> findAll() {
        log.debug("Получен запрос на получение всех фильмов");
        Collection<Film> films = filmStorage.findAll();
        log.info("Возвращено {} фильмов", films.size());
        return films;
    }

    @GetMapping("/{id}")
    public Film findById(@PathVariable Long id) {
        log.debug("Получен запрос на получение фильма с id={}", id);
        Film film = filmStorage.findById(id);
        log.info("Фильм успешно найден: id={}", id);
        return film;
    }

    @PutMapping("/{id}/like/{userId}")
    public ResponseEntity<Void> addLike(@PathVariable Long id, @PathVariable Long userId) {
        log.debug("Получен запрос на добавление лайка: фильм id={}, пользователь id={}", id, userId);
        filmService.addLike(id, userId);
        log.info("Лайк успешно добавлен: фильм id={}, пользователь id={}", id, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/like/{userId}")
    public ResponseEntity<Void> removeLike(@PathVariable Long id, @PathVariable Long userId) {
        log.debug("Получен запрос на удаление лайка: фильм id={}, пользователь id={}", id, userId);
        filmService.removeLike(id, userId);
        log.info("Лайк успешно удален: фильм id={}, пользователь id={}", id, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/popular")
    public List<Film> getTopFilms(@RequestParam(defaultValue = "10") int count) {
        log.debug("Получен запрос на получение топ-{} фильмов", count);
        List<Film> topFilms = filmService.getTopFilms(count);
        log.info("Возвращен список топ-{} фильмов: {} фильмов", count, topFilms.size());
        return topFilms;
    }
}