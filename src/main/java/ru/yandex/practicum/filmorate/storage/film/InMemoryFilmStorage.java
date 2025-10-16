package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;

import jakarta.validation.Validator;
import java.time.LocalDate;
import java.util.*;

@Component
@Profile("test")
@Slf4j
public class InMemoryFilmStorage implements FilmStorage {
    private final Map<Long, Film> films = new HashMap<>();
    private final Validator validator;

    @Autowired
    public InMemoryFilmStorage(Validator validator) {
        this.validator = validator;
    }

    @Override
    public Film create(Film film) {
        log.debug("Создание фильма: {}", film);
        validateFilm(film);
        if (film.getLikes() == null) film.setLikes(new HashSet<>());
        if (film.getGenres() == null) film.setGenres(new HashSet<>());
        if (film.getMpa() == null) film.setMpa(new Mpa(1L, "G"));

        film.setId(getNextId());
        films.put(film.getId(), film);
        log.info("Фильм успешно создан: id={}", film.getId());
        return film;
    }

    @Override
    public Film update(Film newFilm) {
        log.debug("Обновление фильма: {}", newFilm);
        if (newFilm.getId() == null) throw new ValidationException("Id должен быть указан");
        if (!films.containsKey(newFilm.getId())) throw new NotFoundException("Фильм с id " + newFilm.getId() + " не найден");
        validateFilm(newFilm);
        if (newFilm.getLikes() == null) newFilm.setLikes(new HashSet<>());
        if (newFilm.getGenres() == null) newFilm.setGenres(new HashSet<>());
        if (newFilm.getMpa() == null) newFilm.setMpa(new Mpa(1L, "G"));

        Film oldFilm = films.get(newFilm.getId());
        oldFilm.setName(newFilm.getName());
        oldFilm.setDescription(newFilm.getDescription());
        oldFilm.setReleaseDate(newFilm.getReleaseDate());
        oldFilm.setDuration(newFilm.getDuration());
        oldFilm.setMpa(newFilm.getMpa());
        oldFilm.setGenres(newFilm.getGenres());
        oldFilm.setLikes(newFilm.getLikes());
        log.info("Фильм успешно обновлен: id={}", oldFilm.getId());
        return oldFilm;
    }

    @Override
    public Collection<Film> findAll() {
        log.debug("Получение всех фильмов");
        Collection<Film> allFilms = Collections.unmodifiableCollection(new ArrayList<>(films.values()));
        log.info("Возвращено {} фильмов", allFilms.size());
        return allFilms;
    }

    @Override
    public Film findById(Long id) {
        log.debug("Поиск фильма с id={}", id);
        Film film = films.get(id);
        if (film == null) throw new NotFoundException("Фильм с id " + id + " не найден");
        log.info("Фильм найден: id={}", id);
        return film;
    }

    public void clear() {
        log.debug("Очистка хранилища пользователей");
        films.clear();
        log.info("Хранилище пользователей очищено");
    }

    private void validateFilm(Film film) {
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

        LocalDate minReleaseDate = LocalDate.of(1895, 12, 28);
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(minReleaseDate)) {
            log.warn("Дата релиза {} раньше минимальной даты {}", film.getReleaseDate(), minReleaseDate);
            throw new ConditionsNotMetException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }
        log.debug("Валидация фильма успешно пройдена: {}", film);
    }

    private synchronized long getNextId() {
        long currentMaxId = films.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        log.debug("Сгенерирован новый id: {}", currentMaxId + 1);
        return ++currentMaxId;
    }
}