package ru.yandex.practicum.filmorate.storage.film;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static java.util.Set.of;

@JdbcTest
@Import(FilmDbStorage.class)
@Sql(scripts = {"/schema.sql", "/data.sql"})
class FilmDbStorageTest {

    @Autowired
    private FilmDbStorage filmStorage;

    @Test
    void createFilm_ShouldReturnCreatedFilmWithIdAndCorrectFields() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        film.setMpa(new Mpa(3L, "PG-13"));
        film.setGenres(of(new Genre(1L, "Комедия")));
        film.setLikes(of());

        Film created = filmStorage.create(film);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isPositive();
        assertThat(created.getName()).isEqualTo("Test Film");
        assertThat(created.getDescription()).isEqualTo("Test Description");
        assertThat(created.getReleaseDate()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(created.getDuration()).isEqualTo(120);
        assertThat(created.getMpa().getName()).isEqualTo("PG-13");
        assertThat(created.getGenres()).extracting(Genre::getName).containsExactly("Комедия");
        assertThat(created.getLikes()).isEmpty();
    }

    @Test
    void updateFilm_ShouldReturnUpdatedFilmWithCorrectFields() {
        // given
        Film original = filmStorage.create(createTestFilm("Old Film", LocalDate.of(1990, 1, 1), 100, new Mpa(1L, "G"), of()));
        Film updatedFilm = new Film();
        updatedFilm.setId(original.getId());
        updatedFilm.setName("New Film");
        updatedFilm.setDescription("New Description");
        updatedFilm.setReleaseDate(LocalDate.of(2000, 1, 1));
        updatedFilm.setDuration(150);
        updatedFilm.setMpa(new Mpa(4L, "R"));
        updatedFilm.setGenres(of(new Genre(2L, "Драма"), new Genre(4L, "Триллер")));
        updatedFilm.setLikes(of());

        Film updated = filmStorage.update(updatedFilm);

        assertThat(updated).isNotNull();
        assertThat(updated.getId()).isEqualTo(original.getId());
        assertThat(updated.getName()).isEqualTo("New Film");
        assertThat(updated.getDescription()).isEqualTo("New Description");
        assertThat(updated.getReleaseDate()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(updated.getDuration()).isEqualTo(150);
        assertThat(updated.getMpa().getName()).isEqualTo("R");
        assertThat(updated.getGenres()).extracting(Genre::getName).containsExactlyInAnyOrder("Драма", "Триллер");
        assertThat(updated.getLikes()).isEmpty();
    }

    @Test
    void updateFilm_WithNonExistentId_ShouldThrowNotFoundException() {
        Film invalidFilm = new Film();
        invalidFilm.setId(999L);
        invalidFilm.setName("Invalid");
        invalidFilm.setDescription("Desc");
        invalidFilm.setReleaseDate(LocalDate.of(2000, 1, 1));
        invalidFilm.setDuration(100);
        invalidFilm.setMpa(new Mpa(1L, "G"));

        assertThatThrownBy(() -> filmStorage.update(invalidFilm))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Фильм с id 999 не найден");
    }

    @Test
    void findAllFilms_ShouldReturnAllFilms() {
        filmStorage.create(createTestFilm("Film 1", LocalDate.of(2000, 1, 1), 120, new Mpa(2L, "PG"), of()));
        filmStorage.create(createTestFilm("Film 2", LocalDate.of(2010, 6, 15), 150, new Mpa(3L, "PG-13"), of()));

        List<Film> films = filmStorage.findAll().stream().toList();

        assertThat(films).hasSize(2);
        assertThat(films).extracting(Film::getName).containsExactlyInAnyOrder("Film 1", "Film 2");
        assertThat(films).extracting(f -> f.getMpa().getName()).containsExactlyInAnyOrder("PG", "PG-13");
    }

    @Test
    void findFilmById_ShouldReturnFilmWithCorrectFields() {
        Film created = filmStorage.create(createTestFilm("Test Film", LocalDate.of(2000, 1, 1), 120, new Mpa(3L, "PG-13"), of(new Genre(1L, "Комедия"))));

        Film found = filmStorage.findById(created.getId());

        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getName()).isEqualTo("Test Film");
        assertThat(found.getMpa().getName()).isEqualTo("PG-13");
        assertThat(found.getGenres()).extracting(Genre::getName).containsExactly("Комедия");
        assertThat(found.getLikes()).isEmpty();
    }

    @Test
    void findFilmById_WithNonExistentId_ShouldThrowNotFoundException() {
        assertThatThrownBy(() -> filmStorage.findById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Фильм с id 999 не найден");
    }

    private Film createTestFilm(String name, LocalDate releaseDate, int duration, Mpa mpa, Set<Genre> genres) {
        Film film = new Film();
        film.setName(name);
        film.setDescription("Test Desc");
        film.setReleaseDate(releaseDate);
        film.setDuration(duration);
        film.setMpa(mpa);
        film.setGenres(genres != null ? genres : of());
        film.setLikes(of());
        return film;
    }
}