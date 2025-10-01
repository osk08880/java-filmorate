package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    @Autowired
    public FilmService(FilmStorage filmStorage, UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    public void addLike(Long filmId, Long userId) {
        log.debug("Добавление лайка: фильм id={}, пользователь id={}", filmId, userId);
        Film film = filmStorage.findById(filmId);
        User user = userStorage.findById(userId);
        if (user == null) {
            log.warn("Пользователь с id={} не найден", userId);
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }
        if (!film.getLikes().add(userId)) {
            log.warn("Пользователь id={} уже поставил лайк фильму id={}", userId, filmId);
            throw new ValidationException("Пользователь уже поставил лайк этому фильму");
        }
        filmStorage.update(film);
        log.info("Пользователь id={} поставил лайк фильму id={}", userId, filmId);
    }

    public void removeLike(Long filmId, Long userId) {
        log.debug("Удаление лайка: фильм id={}, пользователь id={}", filmId, userId);
        Film film = filmStorage.findById(filmId);
        User user = userStorage.findById(userId);
        if (user == null) {
            log.warn("Пользователь с id={} не найден", userId);
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }
        if (!film.getLikes().contains(userId)) {
            log.warn("Пользователь id={} не лайкал фильм id={}", userId, filmId);
            throw new NotFoundException("Лайк от пользователя id " + userId + " не найден");
        }
        film.getLikes().remove(userId);
        filmStorage.update(film);
        log.info("Пользователь id={} убрал лайк с фильма id={}", userId, filmId);
    }

    public List<Film> getTopFilms(int count) {
        log.debug("Получение топ-{} фильмов", count);
        List<Film> topFilms = filmStorage.findAll().stream()
                .sorted((f1, f2) -> Integer.compare(f2.getLikes().size(), f1.getLikes().size()))
                .limit(count)
                .collect(Collectors.toList());
        log.info("Возвращен список топ-{} фильмов: {} фильмов", count, topFilms.size());
        return topFilms;
    }
}