package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreDao;
import ru.yandex.practicum.filmorate.storage.mpa.MpaDao;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/films")
@Slf4j
public class FilmController {

    private final FilmStorage filmStorage;
    private final FilmService filmService;
    private final Validator validator;
    private final MpaDao mpaDao;
    private final GenreDao genreDao;

    @Autowired
    public FilmController(FilmStorage filmStorage,
                          FilmService filmService,
                          Validator validator,
                          MpaDao mpaDao,
                          GenreDao genreDao) {
        this.filmStorage = filmStorage;
        this.filmService = filmService;
        this.validator = validator;
        this.mpaDao = mpaDao;
        this.genreDao = genreDao;
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

        validateMPA(film);
        validateGenres(film);
        log.debug("Валидация фильма успешно пройдена: {}", film);
    }

    private void validateMPA(Film film) {
        if (film.getMpa() == null || !mpaDao.existsById(film.getMpa().getId())) {
            throw new NotFoundException("MPA с id " + (film.getMpa() != null ? film.getMpa().getId() : null) + " не найден");
        }
    }

    private void validateGenres(Film film) {
        if (film.getGenres() != null) {
            for (var genre : film.getGenres()) {
                if (!genreDao.existsById(genre.getId())) {
                    throw new NotFoundException("Жанр с id " + genre.getId() + " не найден");
                }
            }
        }
    }

    @PostMapping
    public Film create(@Valid @RequestBody Film film) {
        log.debug("Создание фильма: {}", film);
        validateEntity(film);
        return filmStorage.create(film);
    }

    @PutMapping
    public Film update(@Valid @RequestBody Film film) {
        log.debug("Обновление фильма: {}", film);
        validateEntity(film);
        return filmStorage.update(film);
    }

    @GetMapping
    public Collection<Film> findAll() {
        log.debug("Получение всех фильмов");
        return filmStorage.findAll();
    }

    @GetMapping("/{id}")
    public Film findById(@PathVariable Long id) {
        log.debug("Получение фильма по id={}", id);
        return filmStorage.findById(id);
    }

    @PutMapping("/{id}/like/{userId}")
    public ResponseEntity<Void> addLike(@PathVariable Long id, @PathVariable Long userId) {
        log.debug("Добавление лайка: фильм id={}, пользователь id={}", id, userId);
        if (id <= 0) throw new NotFoundException("Фильм с таким id не найден");
        if (userId <= 0) throw new NotFoundException("Пользователь с таким id не найден");
        filmService.addLike(id, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/like/{userId}")
    public ResponseEntity<Void> removeLike(@PathVariable Long id, @PathVariable Long userId) {
        log.debug("Удаление лайка: фильм id={}, пользователь id={}", id, userId);
        if (id <= 0) throw new NotFoundException("Фильм с таким id не найден");
        if (userId <= 0) throw new NotFoundException("Пользователь с таким id не найден");
        filmService.removeLike(id, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/popular")
    public List<Film> getTopFilms(@RequestParam(defaultValue = "10") int count) {
        log.debug("Получение топ-{} фильмов", count);
        return filmService.getTopFilms(count);
    }
}
