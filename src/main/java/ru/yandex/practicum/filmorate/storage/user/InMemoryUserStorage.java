package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.*;

@Component
@Profile("test")
@Slf4j
public class InMemoryUserStorage implements UserStorage {
    private final Map<Long, User> users = new HashMap<>();

    @Override
    public User create(User user) {
        log.debug("Создание пользователя: {}", user);
        validateUser(user);
        if (users.values().stream()
                .anyMatch(u -> u.getEmail().equalsIgnoreCase(user.getEmail()))) {
            log.warn("Email {} уже используется", user.getEmail());
            throw new DuplicatedDataException("Этот имейл уже используется");
        }
        if (user.getName() == null || user.getName().isBlank()) {
            log.debug("Имя пользователя не указано, используется login: {}", user.getLogin());
            user.setName(user.getLogin());
        }
        if (user.getFriends() == null) {
            user.setFriends(new HashMap<>());
        }
        user.setId(getNextId());
        users.put(user.getId(), user);
        log.info("Пользователь успешно создан: id={}", user.getId());
        return user;
    }

    @Override
    public User update(User newUser) {
        log.debug("Обновление пользователя: {}", newUser);
        if (newUser.getId() == null) {
            log.warn("Не указан id пользователя");
            throw new ConditionsNotMetException("Id должен быть указан");
        }
        if (!users.containsKey(newUser.getId())) {
            log.warn("Пользователь с id={} не найден", newUser.getId());
            throw new NotFoundException("Пользователь с id " + newUser.getId() + " не найден");
        }
        validateUser(newUser);
        User oldUser = users.get(newUser.getId());
        if (newUser.getEmail() != null && !newUser.getEmail().equalsIgnoreCase(oldUser.getEmail())) {
            if (users.values().stream()
                    .anyMatch(u -> u.getEmail().equalsIgnoreCase(newUser.getEmail()))) {
                log.warn("Email {} уже используется", newUser.getEmail());
                throw new DuplicatedDataException("Этот имейл уже используется");
            }
            oldUser.setEmail(newUser.getEmail());
        }
        if (newUser.getName() == null || newUser.getName().isBlank()) {
            log.debug("Имя пользователя не указано, используется login: {}", newUser.getLogin());
            oldUser.setName(newUser.getLogin());
        } else {
            oldUser.setName(newUser.getName());
        }
        oldUser.setLogin(newUser.getLogin());
        oldUser.setBirthday(newUser.getBirthday());
        oldUser.setFriends(newUser.getFriends() != null ? newUser.getFriends() : new HashMap<>());
        log.info("Пользователь успешно обновлен: id={}", oldUser.getId());
        return oldUser;
    }

    @Override
    public Collection<User> findAll() {
        log.debug("Получение всех пользователей");
        Collection<User> allUsers = Collections.unmodifiableCollection(new ArrayList<>(users.values()));
        log.info("Возвращено {} пользователей", allUsers.size());
        return allUsers;
    }

    @Override
    public User findById(Long id) {
        log.debug("Поиск пользователя с id={}", id);
        User user = users.get(id);
        if (user == null) {
            log.warn("Пользователь с id={} не найден", id);
            throw new NotFoundException("Пользователь с id " + id + " не найден");
        }
        log.info("Пользователь найден: id={}", id);
        return user;
    }

    public void clear() {
        log.debug("Очистка хранилища пользователей");
        users.clear();
        log.info("Хранилище пользователей очищено");
    }

    private void validateUser(User user) {
        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            throw new ConditionsNotMetException("Дата рождения не может быть в будущем");
        }
    }

    private synchronized long getNextId() {
        long currentMaxId = users.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        log.debug("Сгенерирован новый id: {}", currentMaxId + 1);
        return ++currentMaxId;
    }
}