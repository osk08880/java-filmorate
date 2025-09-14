package ru.yandex.practicum.filmorate;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class UserTest {

    @Autowired
    private Validator validator;

    @Autowired
    private UserController userController;

    @Autowired
    private InMemoryUserStorage userStorage;

    private User getValidUser() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin("validLogin");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return user;
    }

    @BeforeEach
    void setUp() {
        if (validator == null || userController == null || userStorage == null) {
            throw new IllegalStateException("Validator, UserController or UserStorage is not injected. Check Spring context configuration.");
        }
        userStorage.clear(); // Очистка хранилища перед каждым тестом
    }

    @Test
    void shouldFailWhenEmailIsBlank() {
        User user = getValidUser();
        user.setEmail("");
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertThat(violations)
                .anyMatch(v -> v.getMessage().contains("Электронная почта не может быть пустой"));
        assertThrows(ValidationException.class, () -> userController.create(user),
                "Ожидается исключение ValidationException для пустого email");
    }

    @Test
    void shouldFailWhenEmailHasNoAtSymbol() {
        User user = getValidUser();
        user.setEmail("invalidEmail.com");
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertThat(violations)
                .anyMatch(v -> v.getMessage().contains("Некорректный формат email: должен содержать символ @"));
        assertThrows(ValidationException.class, () -> userController.create(user),
                "Ожидается исключение ValidationException для некорректного email");
    }

    @Test
    void shouldFailWhenLoginIsBlank() {
        User user = getValidUser();
        user.setLogin("  ");
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertThat(violations)
                .anyMatch(v -> v.getMessage().contains("Логин не может быть пустым"));
        assertThrows(ValidationException.class, () -> userController.create(user),
                "Ожидается исключение ValidationException для пустого логина");
    }

    @Test
    void shouldFailWhenLoginHasSpaces() {
        User user = getValidUser();
        user.setLogin("bad login");
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertThat(violations)
                .anyMatch(v -> v.getMessage().contains("Логин не может содержать пробелы"));
        assertThrows(ValidationException.class, () -> userController.create(user),
                "Ожидается исключение ValidationException для логина с пробелами");
    }

    @Test
    void shouldFailWhenBirthdayInFuture() {
        User user = getValidUser();
        user.setBirthday(LocalDate.now().plusDays(1));
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertThat(violations)
                .anyMatch(v -> v.getMessage().contains("Дата рождения должна быть в прошлом"));
        assertThrows(ValidationException.class, () -> userController.create(user),
                "Ожидается исключение ValidationException для даты рождения в будущем");
    }

    @Test
    void shouldPassWhenBirthdayIsNull() {
        User user = getValidUser();
        user.setBirthday(null);
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertThat(violations).isEmpty();
        User createdUser = userController.create(user);
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getBirthday()).isNull();
    }

    @Test
    void shouldPassWhenUserIsValid() {
        User user = getValidUser();
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertThat(violations).isEmpty();
        User createdUser = userController.create(user);
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getId()).isNotNull();
    }

    @Test
    void shouldSetNameToLoginWhenNameIsBlank() {
        User user = getValidUser();
        user.setName("");
        User createdUser = userController.create(user);
        assertThat(createdUser.getName()).isEqualTo(user.getLogin());
    }
}