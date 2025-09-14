package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.*;

@Component
@Slf4j
public class InMemoryFilmStorage implements FilmStorage {
    private final Map<Long, Film> films = new HashMap<>();

    @Override
    public Film create(Film film) {
        log.debug("Создание фильма: {}", film);
        LocalDate minReleaseDate = LocalDate.of(1895, 12, 28);
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(minReleaseDate)) {
            log.warn("Дата релиза {} раньше минимальной даты {}", film.getReleaseDate(), minReleaseDate);
            throw new ConditionsNotMetException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }
        film.setId(getNextId());
        films.put(film.getId(), film);
        log.info("Фильм успешно создан: id={}", film.getId());
        return film;
    }

    @Override
    public Film update(Film newFilm) {
        log.debug("Обновление фильма: {}", newFilm);
        if (newFilm.getId() == null) {
            log.warn("Не указан id фильма");
            throw new ConditionsNotMetException("Id должен быть указан");
        }
        if (!films.containsKey(newFilm.getId())) {
            log.warn("Фильм с id={} не найден", newFilm.getId());
            throw new NotFoundException("Фильм с id " + newFilm.getId() + " не найден");
        }
        LocalDate minReleaseDate = LocalDate.of(1895, 12, 28);
        if (newFilm.getReleaseDate() != null && newFilm.getReleaseDate().isBefore(minReleaseDate)) {
            log.warn("Дата релиза {} раньше минимальной даты {}", newFilm.getReleaseDate(), minReleaseDate);
            throw new ConditionsNotMetException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }
        Film oldFilm = films.get(newFilm.getId());
        oldFilm.setName(newFilm.getName());
        oldFilm.setDescription(newFilm.getDescription());
        oldFilm.setReleaseDate(newFilm.getReleaseDate());
        oldFilm.setDuration(newFilm.getDuration());
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
        if (film == null) {
            log.warn("Фильм с id={} не найден", id);
            throw new NotFoundException("Фильм с id " + id + " не найден");
        }
        log.info("Фильм найден: id={}", id);
        return film;
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