package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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

    private User createValidUser() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setLogin("user");
        user.setName("User");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return user;
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
        HttpEntity<Film> filmRequest = new HttpEntity<>(film);
        ResponseEntity<Film> filmResponse = restTemplate.exchange("/films", HttpMethod.POST, filmRequest, Film.class);
        Long filmId = filmResponse.getBody().getId();

        User user = createValidUser();
        HttpEntity<User> userRequest = new HttpEntity<>(user);
        ResponseEntity<User> userResponse = restTemplate.exchange("/users", HttpMethod.POST, userRequest, User.class);
        Long userId = userResponse.getBody().getId();

        ResponseEntity<Void> addLikeResponse = restTemplate.exchange(
                "/films/" + filmId + "/like/" + userId,
                HttpMethod.PUT,
                null,
                Void.class
        );
        assertThat(addLikeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Film> getResponse = restTemplate.getForEntity("/films/" + filmId, Film.class);
        assertThat(getResponse.getBody().getLikes()).contains(userId);

        restTemplate.delete("/films/" + filmId + "/like/" + userId);
        getResponse = restTemplate.getForEntity("/films/" + filmId, Film.class);
        assertThat(getResponse.getBody().getLikes()).doesNotContain(userId);
    }

    @Test
    void shouldReturn404WhenAddingLikeWithNonExistingFilmOrUser() {
        ResponseEntity<Map> responseFilm = restTemplate.exchange(
                "/films/999/like/1",
                HttpMethod.PUT,
                null,
                Map.class
        );
        assertThat(responseFilm.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseFilm.getBody().get("error")).isEqualTo("Not found");
        assertThat(responseFilm.getBody().get("message")).isEqualTo("Фильм с id 999 не найден");

        Film film = createValidFilm();
        HttpEntity<Film> filmRequest = new HttpEntity<>(film);
        ResponseEntity<Film> filmResponse = restTemplate.exchange("/films", HttpMethod.POST, filmRequest, Film.class);
        Long filmId = filmResponse.getBody().getId();

        ResponseEntity<Map> responseUser = restTemplate.exchange(
                "/films/" + filmId + "/like/999",
                HttpMethod.PUT,
                null,
                Map.class
        );
        assertThat(responseUser.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseUser.getBody().get("error")).isEqualTo("Not found");
        assertThat(responseUser.getBody().get("message")).isEqualTo("Пользователь с id 999 не найден");
    }

    @Test
    void shouldGetTopFilms() {
        Film film1 = createValidFilm();
        Film film2 = createValidFilm();
        film2.setName("Another Film");
        ResponseEntity<Film> film1Response = restTemplate.exchange("/films", HttpMethod.POST, new HttpEntity<>(film1), Film.class);
        ResponseEntity<Film> film2Response = restTemplate.exchange("/films", HttpMethod.POST, new HttpEntity<>(film2), Film.class);

        User user = createValidUser();
        HttpEntity<User> userRequest = new HttpEntity<>(user);
        ResponseEntity<User> userResponse = restTemplate.exchange("/users", HttpMethod.POST, userRequest, User.class);
        Long userId = userResponse.getBody().getId();
        restTemplate.put("/films/" + film1Response.getBody().getId() + "/like/" + userId, null);

        ResponseEntity<List> response = restTemplate.getForEntity("/films/popular?count=2", List.class);
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(2);
    }
}