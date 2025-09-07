package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;

@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {
    private final Map<Long, User> users = new HashMap<>();
    private final Validator validator;

    @Autowired
    public UserController(Validator validator) {
        if (validator == null) {
            throw new IllegalStateException("Validator не внедрён. Проверь конфигурацию контекста Spring.");
        }
        this.validator = validator;
    }

    private void validateEntity(User user) {
        var violations = validator.validate(user);
        if (!violations.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder();
            violations.forEach(violation -> {
                String fieldName = violation.getPropertyPath().toString();
                String errorMsg = violation.getMessage();
                errorMessage.append(fieldName).append(": ").append(errorMsg).append("; ");
                log.warn("Ошибка валидации в UserController: поле '{}' - {}", fieldName, errorMsg);
            });
            throw new ValidationException(errorMessage.toString());
        }
    }

    @PostMapping
    public User create(@Valid @RequestBody User user) {
        try {
            validateEntity(user);

            if (users.values().stream()
                    .anyMatch(u -> u.getEmail().equalsIgnoreCase(user.getEmail()))) {
                log.warn("Ошибка создания пользователя: email {} уже используется", user.getEmail());
                throw new DuplicatedDataException("Этот имейл уже используется");
            }
            if (user.getName() == null || user.getName().isBlank()) {
                log.info("Имя пользователя не указано, используется login: {}", user.getLogin());
                user.setName(user.getLogin());
            }
            user.setId(getNextId());
            users.put(user.getId(), user);

            log.info("Пользователь успешно создан: id={}, email={}, login={}",
                    user.getId(), user.getEmail(), user.getLogin());
            return user;
        } catch (ValidationException e) {
            throw e;
        }
    }

    @PutMapping
    public User update(@Valid @RequestBody User newUser) {
        try {
            validateEntity(newUser);

            if (newUser.getId() == null) {
                log.warn("Ошибка обновления пользователя: не указан id");
                throw new ConditionsNotMetException("Id должен быть указан");
            }
            if (!users.containsKey(newUser.getId())) {
                log.warn("Ошибка обновления пользователя: пользователь с id={} не найден", newUser.getId());
                throw new NotFoundException("Пользователь с id " + newUser.getId() + " не найден");
            }

            User oldUser = users.get(newUser.getId());
            if (newUser.getEmail() != null && !newUser.getEmail().equalsIgnoreCase(oldUser.getEmail())) {
                if (users.values().stream()
                        .anyMatch(u -> u.getEmail().equalsIgnoreCase(newUser.getEmail()))) {
                    log.warn("Ошибка обновления пользователя: email {} уже используется", newUser.getEmail());
                    throw new DuplicatedDataException("Этот имейл уже используется");
                }
                oldUser.setEmail(newUser.getEmail());
            }
            if (newUser.getName() == null || newUser.getName().isBlank()) {
                log.info("Имя пользователя не указано, используется login: {}", newUser.getLogin());
                oldUser.setName(newUser.getLogin());
            } else {
                oldUser.setName(newUser.getName());
            }

            oldUser.setLogin(newUser.getLogin());
            oldUser.setBirthday(newUser.getBirthday());

            log.info("Пользователь успешно обновлен: id={}, email={}, login={}",
                    oldUser.getId(), oldUser.getEmail(), oldUser.getLogin());
            return oldUser;
        } catch (ValidationException e) {
            throw e;
        }
    }

    @GetMapping
    public Collection<User> findAll() {
        return Collections.unmodifiableCollection(new ArrayList<>(users.values()));
    }

    private synchronized long getNextId() {
        long currentMaxId = users.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }
}
