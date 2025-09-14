package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FilmControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private Film createValidFilm() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("Test description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        return film;
    }

    @Test
    void shouldCreateAndFindFilm() {
        Film film = createValidFilm();
        HttpEntity<Film> request = new HttpEntity<>(film);
        ResponseEntity<Film> response = restTemplate.exchange("/films", HttpMethod.POST, request, Film.class);
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();

        ResponseEntity<Film> getResponse = restTemplate.getForEntity("/films/" + response.getBody().getId(), Film.class);
        assertThat(getResponse.getStatusCodeValue()).isEqualTo(200);
        assertThat(getResponse.getBody().getName()).isEqualTo("Test Film");
    }

    @Test
    void shouldAddAndRemoveLike() {
        Film film = createValidFilm();
        HttpEntity<Film> request = new HttpEntity<>(film);
        ResponseEntity<Film> filmResponse = restTemplate.exchange("/films", HttpMethod.POST, request, Film.class);
        Long filmId = filmResponse.getBody().getId();

        restTemplate.put("/films/" + filmId + "/like/1", null);
        ResponseEntity<Film> getResponse = restTemplate.getForEntity("/films/" + filmId, Film.class);
        assertThat(getResponse.getBody().getLikes()).contains(1L);

        restTemplate.delete("/films/" + filmId + "/like/1");
        getResponse = restTemplate.getForEntity("/films/" + filmId, Film.class);
        assertThat(getResponse.getBody().getLikes()).doesNotContain(1L);
    }

    @Test
    void shouldGetTopFilms() {
        Film film1 = createValidFilm();
        Film film2 = createValidFilm();
        film2.setName("Another Film");
        restTemplate.exchange("/films", HttpMethod.POST, new HttpEntity<>(film1), Film.class);
        restTemplate.exchange("/films", HttpMethod.POST, new HttpEntity<>(film2), Film.class);

        restTemplate.put("/films/1/like/1", null);

        ResponseEntity<List> response = restTemplate.getForEntity("/films/popular?count=2", List.class);
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(2);
    }
}