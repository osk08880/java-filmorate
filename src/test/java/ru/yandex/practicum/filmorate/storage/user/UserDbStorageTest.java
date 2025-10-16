package ru.yandex.practicum.filmorate.storage.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@Import(UserDbStorage.class)
@Sql(scripts = {"/schema.sql"})
class UserDbStorageTest {

    @Autowired
    private UserDbStorage userStorage;

    @Test
    void createUser_ShouldReturnCreatedUserWithIdAndCorrectFields() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin("testlogin");
        user.setName("Test Name");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        user.setFriends(Map.of());

        User created = userStorage.create(user);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isPositive();
        assertThat(created.getEmail()).isEqualTo("test@example.com");
        assertThat(created.getLogin()).isEqualTo("testlogin");
        assertThat(created.getName()).isEqualTo("Test Name");
        assertThat(created.getBirthday()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(created.getFriends()).isEmpty();
    }

    @Test
    void updateUser_ShouldReturnUpdatedUserWithCorrectFields() {
        User original = userStorage.create(createTestUser("old@example.com", "oldlogin", "Old Name", LocalDate.of(1980, 1, 1)));
        User friendUser = new User();
        friendUser.setEmail("friend@example.com");
        friendUser.setLogin("friendlogin");
        friendUser.setName("Friend Name");
        friendUser.setBirthday(LocalDate.of(1990, 1, 1));
        User createdFriend = userStorage.create(friendUser);

        User updatedUser = new User();
        updatedUser.setId(original.getId());
        updatedUser.setEmail("new@example.com");
        updatedUser.setLogin("newlogin");
        updatedUser.setName("New Name");
        updatedUser.setBirthday(LocalDate.of(1990, 1, 1));
        updatedUser.setFriends(Map.of(createdFriend.getId(), User.FriendshipStatus.UNCONFIRMED));

        User updated = userStorage.update(updatedUser);

        assertThat(updated).isNotNull();
        assertThat(updated.getEmail()).isEqualTo("new@example.com");
        assertThat(updated.getName()).isEqualTo("New Name");
        assertThat(updated.getFriends()).hasSize(1).containsKey(createdFriend.getId());
    }

    @Test
    void findAllUsers_ShouldReturnAllUsers() {
        userStorage.create(createTestUser("user1@example.com", "user1", "User 1", LocalDate.of(1990, 1, 1)));
        userStorage.create(createTestUser("user2@example.com", "user2", "User 2", LocalDate.of(1985, 6, 15)));

        List<User> users = userStorage.findAll().stream().toList();

        assertThat(users).hasSize(2);
        assertThat(users).extracting(User::getEmail).containsExactlyInAnyOrder("user1@example.com", "user2@example.com");
    }

    @Test
    void findUserById_ShouldReturnUserWithCorrectFields() {
        User created = userStorage.create(createTestUser("test@example.com", "testlogin", "Test Name", LocalDate.of(1990, 1, 1)));

        User found = userStorage.findById(created.getId());

        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getEmail()).isEqualTo("test@example.com");
        assertThat(found.getName()).isEqualTo("Test Name");
        assertThat(found.getFriends()).isEmpty();
    }

    @Test
    void findUserById_WithNonExistentId_ShouldThrowNotFoundException() {
        assertThatThrownBy(() -> userStorage.findById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Пользователь с id 999 не найден");
    }

    @Test
    void createUser_WithFutureBirthday_ShouldThrowConditionsNotMetException() {
        User invalidUser = new User();
        invalidUser.setEmail("invalid@example.com");
        invalidUser.setLogin("invalid");
        invalidUser.setBirthday(LocalDate.of(2030, 1, 1));

        assertThatThrownBy(() -> userStorage.create(invalidUser))
                .isInstanceOf(ConditionsNotMetException.class)
                .hasMessage("Дата рождения не может быть в будущем");
    }

    @Test
    void createUser_WithDuplicateEmail_ShouldThrowDuplicatedDataException() {
        String email = "duplicate@example.com";
        userStorage.create(createTestUser(email, "first", "First", LocalDate.of(1990, 1, 1)));

        User duplicateUser = createTestUser(email, "second", "Second", LocalDate.of(1990, 1, 1));

        assertThatThrownBy(() -> userStorage.create(duplicateUser))
                .isInstanceOf(DuplicatedDataException.class)
                .hasMessage("Этот имейл уже используется");
    }

    private User createTestUser(String email, String login, String name, LocalDate birthday) {
        User user = new User();
        user.setEmail(email);
        user.setLogin(login);
        user.setName(name);
        user.setBirthday(birthday);
        user.setFriends(Map.of());
        return user;
    }
}