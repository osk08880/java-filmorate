package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private InMemoryUserStorage userStorage;

    private User createValidUser() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin("testLogin");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return user;
    }

    @BeforeEach
    void setUp() {
        userStorage.clear(); // Очистка хранилища перед каждым тестом
    }

    @Test
    void shouldCreateAndFindUser() {
        User user = createValidUser();
        HttpEntity<User> request = new HttpEntity<>(user);
        ResponseEntity<User> response = restTemplate.exchange("/users", HttpMethod.POST, request, User.class);
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();

        ResponseEntity<User> getResponse = restTemplate.getForEntity("/users/" + response.getBody().getId(), User.class);
        assertThat(getResponse.getStatusCodeValue()).isEqualTo(200);
        assertThat(getResponse.getBody().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void shouldFailCreateUserWithDuplicateEmail() {
        User user1 = createValidUser();
        restTemplate.exchange("/users", HttpMethod.POST, new HttpEntity<>(user1), User.class);

        User user2 = createValidUser();
        user2.setLogin("anotherLogin");
        HttpEntity<User> request = new HttpEntity<>(user2);
        ResponseEntity<String> response = restTemplate.exchange("/users", HttpMethod.POST, request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Этот имейл уже используется");
    }

    @Test
    void shouldUpdateUser() {
        User user = createValidUser();
        ResponseEntity<User> createResponse = restTemplate.exchange("/users", HttpMethod.POST, new HttpEntity<>(user), User.class);
        Long userId = createResponse.getBody().getId();

        User updatedUser = createValidUser();
        updatedUser.setId(userId);
        updatedUser.setEmail("updated@example.com");
        updatedUser.setName("Updated Name");
        HttpEntity<User> request = new HttpEntity<>(updatedUser);
        ResponseEntity<User> updateResponse = restTemplate.exchange("/users", HttpMethod.PUT, request, User.class);
        assertThat(updateResponse.getStatusCodeValue()).isEqualTo(200);
        assertThat(updateResponse.getBody().getEmail()).isEqualTo("updated@example.com");
        assertThat(updateResponse.getBody().getName()).isEqualTo("Updated Name");
    }

    @Test
    void shouldAddAndRemoveFriend() {
        User user1 = createValidUser();
        User user2 = createValidUser();
        user2.setEmail("friend@example.com");
        user2.setLogin("friendLogin");
        ResponseEntity<User> response1 = restTemplate.exchange("/users", HttpMethod.POST, new HttpEntity<>(user1), User.class);
        ResponseEntity<User> response2 = restTemplate.exchange("/users", HttpMethod.POST, new HttpEntity<>(user2), User.class);
        Long userId = response1.getBody().getId();
        Long friendId = response2.getBody().getId();

        ResponseEntity<Void> addFriendResponse = restTemplate.exchange(
                "/users/" + userId + "/friends/" + friendId,
                HttpMethod.PUT,
                new HttpEntity<>(null),
                Void.class
        );
        assertThat(addFriendResponse.getStatusCodeValue()).isEqualTo(200);

        ResponseEntity<List<User>> friendsResponse = restTemplate.exchange(
                "/users/" + userId + "/friends",
                HttpMethod.GET,
                null,
                new org.springframework.core.ParameterizedTypeReference<List<User>>() {}
        );
        assertThat(friendsResponse.getStatusCodeValue()).isEqualTo(200);
        assertThat(friendsResponse.getBody()).hasSize(1);
        assertThat(friendsResponse.getBody().get(0).getId()).isEqualTo(friendId);

        ResponseEntity<Void> removeFriendResponse = restTemplate.exchange(
                "/users/" + userId + "/friends/" + friendId,
                HttpMethod.DELETE,
                null,
                Void.class
        );
        assertThat(removeFriendResponse.getStatusCodeValue()).isEqualTo(200);

        friendsResponse = restTemplate.exchange(
                "/users/" + userId + "/friends",
                HttpMethod.GET,
                null,
                new org.springframework.core.ParameterizedTypeReference<List<User>>() {}
        );
        assertThat(friendsResponse.getStatusCodeValue()).isEqualTo(200);
        assertThat(friendsResponse.getBody()).isEmpty();
    }

    @Test
    void shouldGetCommonFriends() {
        User user1 = createValidUser();
        User user2 = createValidUser();
        user2.setEmail("friend1@example.com");
        user2.setLogin("friend1Login");
        User user3 = createValidUser();
        user3.setEmail("friend2@example.com");
        user3.setLogin("friend2Login");
        ResponseEntity<User> response1 = restTemplate.exchange("/users", HttpMethod.POST, new HttpEntity<>(user1), User.class);
        ResponseEntity<User> response2 = restTemplate.exchange("/users", HttpMethod.POST, new HttpEntity<>(user2), User.class);
        ResponseEntity<User> response3 = restTemplate.exchange("/users", HttpMethod.POST, new HttpEntity<>(user3), User.class);
        Long userId1 = response1.getBody().getId();
        Long userId2 = response2.getBody().getId();
        Long userId3 = response3.getBody().getId();

        restTemplate.exchange("/users/" + userId1 + "/friends/" + userId3, HttpMethod.PUT, new HttpEntity<>(null), Void.class);
        restTemplate.exchange("/users/" + userId2 + "/friends/" + userId3, HttpMethod.PUT, new HttpEntity<>(null), Void.class);

        ResponseEntity<List<User>> commonFriendsResponse = restTemplate.exchange(
                "/users/" + userId1 + "/friends/common/" + userId2,
                HttpMethod.GET,
                null,
                new org.springframework.core.ParameterizedTypeReference<List<User>>() {}
        );
        assertThat(commonFriendsResponse.getStatusCodeValue()).isEqualTo(200);
        assertThat(commonFriendsResponse.getBody()).hasSize(1);
        assertThat(commonFriendsResponse.getBody().get(0).getId()).isEqualTo(userId3);
    }

    @Test
    void shouldFailAddFriendWithNonExistentId() {
        User user = createValidUser();
        ResponseEntity<User> response = restTemplate.exchange("/users", HttpMethod.POST, new HttpEntity<>(user), User.class);
        Long userId = response.getBody().getId();

        ResponseEntity<String> friendResponse = restTemplate.exchange(
                "/users/" + userId + "/friends/999",
                HttpMethod.PUT,
                new HttpEntity<>(null),
                String.class
        );
        assertThat(friendResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(friendResponse.getBody()).contains("Пользователь с id 999 не найден");
    }
}