package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.util.*;
import java.util.stream.Collectors;

@Component("filmDb")
@Profile("!test")
@Slf4j
public class FilmDbStorage implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;

    public FilmDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Film create(Film film) {
        log.debug("Создание фильма в БД: {}", film);

        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_rating_id) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setDate(3, Date.valueOf(film.getReleaseDate()));
            ps.setInt(4, film.getDuration());
            ps.setLong(5, getMpaId(film.getMpa()));
            return ps;
        }, keyHolder);
        Long id = keyHolder.getKey().longValue();
        film.setId(id);
        saveGenres(id, film.getGenres());
        saveLikes(id, film.getLikes());
        log.info("Фильм успешно создан в БД: id={}", id);
        return findById(id);
    }

    @Override
    public Film update(Film newFilm) {
        log.debug("Обновление фильма в БД: {}", newFilm);
        if (newFilm.getId() == null) {
            throw new ConditionsNotMetException("Id должен быть указан");
        }
        findById(newFilm.getId());

        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_rating_id = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                newFilm.getName(),
                newFilm.getDescription(),
                Date.valueOf(newFilm.getReleaseDate()),
                newFilm.getDuration(),
                getMpaId(newFilm.getMpa()),
                newFilm.getId());
        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", newFilm.getId());
        saveGenres(newFilm.getId(), newFilm.getGenres());
        jdbcTemplate.update("DELETE FROM likes WHERE film_id = ?", newFilm.getId());
        saveLikes(newFilm.getId(), newFilm.getLikes());
        log.info("Фильм успешно обновлен в БД: id={}", newFilm.getId());
        return findById(newFilm.getId());
    }

    @Override
    public Collection<Film> findAll() {
        log.debug("Получение всех фильмов из БД");
        String sql = "SELECT f.id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id, m.rating " +
                "FROM films f LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id";
        List<Film> films = jdbcTemplate.query(sql, filmRowMapper());
        films.forEach(f -> {
            loadGenres(f);
            loadLikes(f);
        });
        log.info("Возвращено {} фильмов из БД", films.size());
        return films;
    }

    @Override
    public Film findById(Long id) {
        log.debug("Поиск фильма в БД по id={}", id);
        String sql = "SELECT f.id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id, m.rating " +
                "FROM films f LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id WHERE f.id = ?";
        try {
            Film film = jdbcTemplate.queryForObject(sql, filmRowMapper(), id);
            if (film != null) {
                loadGenres(film);
                loadLikes(film);
                log.info("Фильм найден в БД: id={}", id);
                return film;
            }
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            log.warn("Фильм с id={} не найден в БД", id);
            throw new NotFoundException("Фильм с id " + id + " не найден");
        }
        throw new NotFoundException("Фильм с id " + id + " не найден");
    }

    private Long getMpaId(Mpa mpa) {
        if (mpa == null) return 1L;

        if (mpa.getId() != null) {
            List<Long> ids = jdbcTemplate.queryForList(
                    "SELECT id FROM mpa_ratings WHERE id = ?",
                    Long.class,
                    mpa.getId()
            );
            if (!ids.isEmpty()) return ids.get(0);
        }

        if (mpa.getName() != null) {
            List<Long> ids = jdbcTemplate.queryForList(
                    "SELECT id FROM mpa_ratings WHERE rating = ?",
                    Long.class,
                    mpa.getName()
            );
            if (!ids.isEmpty()) return ids.get(0);
        }

        log.warn("MPA рейтинг '{}' не найден в БД, используем default G (ID=1)", mpa);
        return 1L;
    }

    private void saveGenres(Long filmId, Set<Genre> genres) {
        if (genres == null || genres.isEmpty()) return;

        List<Genre> invalidGenres = new ArrayList<>();

        Set<Long> candidateIds = genres.stream()
                .filter(g -> g.getId() != null)
                .map(Genre::getId)
                .collect(Collectors.toSet());

        Set<String> candidateNames = genres.stream()
                .filter(g -> g.getName() != null && g.getId() == null)
                .map(Genre::getName)
                .collect(Collectors.toSet());

        Map<Long, Long> validIdsById = new HashMap<>();
        if (!candidateIds.isEmpty()) {
            String idSql = "SELECT id FROM genres WHERE id IN (" + String.join(",", Collections.nCopies(candidateIds.size(), "?")) + ")";
            try {
                List<Long> validIds = jdbcTemplate.queryForList(idSql, Long.class, candidateIds.toArray());
                validIds.forEach(id -> validIdsById.put(id, id));
            } catch (EmptyResultDataAccessException ignored) {
            }
        }

        Map<String, Long> validIdsByName = new HashMap<>();
        if (!candidateNames.isEmpty()) {
            String nameSql = "SELECT id, name FROM genres WHERE name IN (" + String.join(",", Collections.nCopies(candidateNames.size(), "?")) + ")";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(nameSql, candidateNames.toArray());
            rows.forEach(row -> {
                Long id = (Long) row.get("id");
                String name = (String) row.get("name");
                validIdsByName.put(name, id);
            });
        }

        List<Object[]> batchArgs = new ArrayList<>();
        for (Genre genre : genres) {
            Long genreId = null;
            if (genre.getId() != null && validIdsById.containsKey(genre.getId())) {
                genreId = genre.getId();
            } else if (genre.getName() != null && validIdsByName.containsKey(genre.getName())) {
                genreId = validIdsByName.get(genre.getName());
            }
            if (genreId != null) {
                batchArgs.add(new Object[]{filmId, genreId});
            } else {
                invalidGenres.add(genre);
            }
        }

        if (!batchArgs.isEmpty()) {
            String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
            int[] updateCounts = jdbcTemplate.batchUpdate(sql, batchArgs);
            log.debug("Сохранено {} жанров для фильма id={}", updateCounts.length, filmId);
        }

        if (!invalidGenres.isEmpty()) {
            log.warn("Жанры не найдены в БД и пропущены для фильма id={}: {}", filmId, invalidGenres);
        }
    }

    private void saveLikes(Long filmId, Set<Long> likes) {
        if (likes == null || likes.isEmpty()) return;
        String sql = "INSERT INTO likes (user_id, film_id) VALUES (?, ?)";
        for (Long userId : likes) {
            jdbcTemplate.update(sql, userId, filmId);
        }
    }

    private void loadGenres(Film film) {
        String sql = "SELECT g.id, g.name FROM genres g JOIN film_genres fg ON g.id = fg.genre_id WHERE fg.film_id = ?";
        List<Genre> genres = jdbcTemplate.query(sql, (rs, rowNum) -> new Genre(rs.getLong("id"), rs.getString("name")), film.getId());
        genres.sort(Comparator.comparingLong(Genre::getId));
        film.setGenres(new LinkedHashSet<>(genres));
    }

    private void loadLikes(Film film) {
        String sql = "SELECT user_id FROM likes WHERE film_id = ?";
        Set<Long> likes = new HashSet<>(jdbcTemplate.queryForList(sql, Long.class, film.getId()));
        film.setLikes(likes);
    }

    private RowMapper<Film> filmRowMapper() {
        return (rs, rowNum) -> {
            Film film = new Film();
            film.setId(rs.getLong("id"));
            film.setName(rs.getString("name"));
            film.setDescription(rs.getString("description"));
            film.setReleaseDate(rs.getDate("release_date").toLocalDate());
            film.setDuration(rs.getInt("duration"));
            Long mpaId = rs.getLong("mpa_rating_id");
            String ratingName = rs.getString("rating");
            if (ratingName != null) {
                film.setMpa(new Mpa(mpaId, ratingName));
            } else {
                film.setMpa(new Mpa(1L, "G"));
            }
            return film;
        };
    }
}