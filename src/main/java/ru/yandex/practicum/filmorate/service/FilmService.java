package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FilmService {
    private final FilmStorage filmStorage;

    @Autowired
    public FilmService(FilmStorage filmStorage) {
        this.filmStorage = filmStorage;
    }

    public void addLike(Long filmId, Long userId) {
        log.debug("Добавление лайка: фильм id={}, пользователь id={}", filmId, userId);
        Film film = filmStorage.findById(filmId);
        log.debug("Найден фильм id={}", filmId);
        film.getLikes().add(userId);
        log.info("Пользователь id={} поставил лайк фильму id={}", userId, filmId);
    }

    public void removeLike(Long filmId, Long userId) {
        log.debug("Удаление лайка: фильм id={}, пользователь id={}", filmId, userId);
        Film film = filmStorage.findById(filmId);
        log.debug("Найден фильм id={}", filmId);
        if (!film.getLikes().contains(userId)) {
            log.warn("Пользователь id={} не лайкал фильм id={}", userId, filmId);
            throw new NotFoundException("Лайк от пользователя id " + userId + " не найден");
        }
        film.getLikes().remove(userId);
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