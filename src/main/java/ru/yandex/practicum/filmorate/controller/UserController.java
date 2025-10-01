package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.*;

@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {
    private final UserStorage userStorage;
    private final UserService userService;
    private final Validator validator;

    @Autowired
    public UserController(UserStorage userStorage, UserService userService, Validator validator) {
        this.userStorage = userStorage;
        this.userService = userService;
        this.validator = validator;
    }

    private void validateEntity(User user) {
        log.debug("Начало валидации пользователя: {}", user);
        var violations = validator.validate(user);
        if (!violations.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder();
            violations.forEach(violation -> {
                String fieldName = violation.getPropertyPath().toString();
                String errorMsg = violation.getMessage();
                errorMessage.append(fieldName).append(": ").append(errorMsg).append("; ");
                log.warn("Ошибка валидации: поле '{}' - {}", fieldName, errorMsg);
            });
            throw new ValidationException(errorMessage.toString());
        }
        log.debug("Валидация пользователя успешно пройдена: {}", user);
    }

    @PostMapping
    public User create(@Valid @RequestBody User user) {
        log.debug("Получен запрос на создание пользователя: {}", user);
        validateEntity(user);
        User createdUser = userStorage.create(user);
        log.info("Пользователь успешно создан: id={}", createdUser.getId());
        return createdUser;
    }

    @PutMapping
    public User update(@Valid @RequestBody User user) {
        log.debug("Получен запрос на обновление пользователя: {}", user);
        validateEntity(user);
        User updatedUser = userStorage.update(user);
        log.info("Пользователь успешно обновлен: id={}", updatedUser.getId());
        return updatedUser;
    }

    @GetMapping
    public Collection<User> findAll() {
        log.debug("Получен запрос на получение всех пользователей");
        Collection<User> users = userStorage.findAll();
        log.info("Возвращено {} пользователей", users.size());
        return users;
    }

    @GetMapping("/{id}")
    public User findById(@PathVariable Long id) {
        log.debug("Получен запрос на получение пользователя с id={}", id);
        User user = userStorage.findById(id);
        log.info("Пользователь успешно найден: id={}", id);
        return user;
    }

    @PutMapping("/{id}/friends/{friendId}")
    public void addFriend(@PathVariable Long id, @PathVariable Long friendId) {
        log.debug("Получен запрос на добавление друга: пользователь id={}, друг id={}", id, friendId);
        userService.addFriend(id, friendId);
        log.info("Друг успешно добавлен: пользователь id={}, друг id={}", id, friendId);
    }

    @DeleteMapping("/{id}/friends/{friendId}")
    public void removeFriend(@PathVariable Long id, @PathVariable Long friendId) {
        log.debug("Получен запрос на удаление друга: пользователь id={}, друг id={}", id, friendId);
        userService.removeFriend(id, friendId);
        log.info("Друг успешно удален: пользователь id={}, друг id={}", id, friendId);
    }

    @GetMapping("/{id}/friends")
    public List<User> getFriends(@PathVariable Long id) {
        log.debug("Получен запрос на получение списка друзей пользователя id={}", id);
        List<User> friends = userService.getFriends(id);
        log.info("Возвращен список друзей для пользователя id={}: {} друзей", id, friends.size());
        return friends;
    }

    @GetMapping("/{id}/friends/common/{otherId}")
    public List<User> getCommonFriends(@PathVariable Long id, @PathVariable Long otherId) {
        log.debug("Получен запрос на получение общих друзей: пользователь id={}, другой пользователь id={}", id, otherId);
        List<User> commonFriends = userService.getCommonFriends(id, otherId);
        log.info("Возвращен список общих друзей для пользователей id={} и id={}: {} друзей", id, otherId, commonFriends.size());
        return commonFriends;
    }
}