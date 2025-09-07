package ru.yandex.practicum.filmorate;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UserTest {

    @Autowired
    private Validator validator;

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
        if (validator == null) {
            throw new IllegalStateException("Validator is not injected. Check Spring context configuration.");
        }
    }

    @Test
    void shouldFailWhenEmailIsBlank() {
        User user = getValidUser();
        user.setEmail("");
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertThat(violations)
                .anyMatch(v -> v.getMessage().contains("Электронная почта не может быть пустой"));
    }

    @Test
    void shouldFailWhenEmailHasNoAtSymbol() {
        User user = getValidUser();
        user.setEmail("invalidEmail.com");
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertThat(violations)
                .anyMatch(v -> v.getMessage().contains("Некорректный формат email: должен содержать символ @"));
    }

    @Test
    void shouldFailWhenLoginIsBlank() {
        User user = getValidUser();
        user.setLogin("  ");
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertThat(violations)
                .anyMatch(v -> v.getMessage().contains("Логин не может быть пустым"));
    }

    @Test
    void shouldFailWhenLoginHasSpaces() {
        User user = getValidUser();
        user.setLogin("bad login");
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertThat(violations)
                .anyMatch(v -> v.getMessage().contains("Логин не может содержать пробелы"));
    }

    @Test
    void shouldFailWhenBirthdayInFuture() {
        User user = getValidUser();
        user.setBirthday(LocalDate.now().plusDays(1));
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertThat(violations)
                .anyMatch(v -> v.getMessage().contains("Дата рождения должна быть в прошлом"));
    }

    @Test
    void shouldPassWhenUserIsValid() {
        User user = getValidUser();
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertThat(violations).isEmpty();
    }
}