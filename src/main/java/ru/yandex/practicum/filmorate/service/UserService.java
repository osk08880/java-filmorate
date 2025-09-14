package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService {
    private final UserStorage userStorage;

    @Autowired
    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public void addFriend(Long userId, Long friendId) {
        log.debug("Добавление друга: пользователь id={}, друг id={}", userId, friendId);
        User user = userStorage.findById(userId);
        User friend = userStorage.findById(friendId);
        log.debug("Найдены пользователи: пользователь id={}, друг id={}", userId, friendId);

        user.getFriends().add(friendId);
        friend.getFriends().add(userId);
        log.info("Пользователь id={} добавил в друзья пользователя id={}", userId, friendId);
    }

    public void removeFriend(Long userId, Long friendId) {
        log.debug("Удаление друга: пользователь id={}, друг id={}", userId, friendId);
        User user = userStorage.findById(userId);
        User friend = userStorage.findById(friendId);
        log.debug("Найдены пользователи: пользователь id={}, друг id={}", userId, friendId);

        if (!user.getFriends().contains(friendId) || !friend.getFriends().contains(userId)) {
            log.warn("Пользователь id={} не является другом пользователя id={}", friendId, userId);
            throw new NotFoundException("Пользователь с id " + friendId + " не является другом");
        }

        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId);
        log.info("Пользователь id={} удалил из друзей пользователя id={}", userId, friendId);
    }

    public List<User> getCommonFriends(Long userId, Long otherUserId) {
        log.debug("Получение общих друзей: пользователь id={}, другой пользователь id={}", userId, otherUserId);
        User user = userStorage.findById(userId);
        User otherUser = userStorage.findById(otherUserId);
        log.debug("Найдены пользователи: пользователь id={}, другой пользователь id={}", userId, otherUserId);

        Set<Long> commonFriendIds = new HashSet<>(user.getFriends());
        commonFriendIds.retainAll(otherUser.getFriends());
        log.debug("Найдены общие друзья: {}", commonFriendIds);

        List<User> commonFriends = commonFriendIds.stream()
                .map(userStorage::findById)
                .collect(Collectors.toList());
        log.info("Возвращен список общих друзей для пользователей id={} и id={}: {} друзей",
                userId, otherUserId, commonFriends.size());
        return commonFriends;
    }

    public List<User> getFriends(Long userId) {
        log.debug("Получение списка друзей пользователя id={}", userId);
        User user = userStorage.findById(userId);
        log.debug("Найден пользователь id={}", userId);

        if (user.getFriends().isEmpty()) {
            log.warn("Список друзей пользователя id={} пуст", userId);
            throw new NotFoundException("У пользователя с id " + userId + " нет друзей");
        }

        List<User> friends = user.getFriends().stream()
                .map(userStorage::findById)
                .collect(Collectors.toList());
        log.info("Возвращен список друзей для пользователя id={}: {} друзей", userId, friends.size());
        return friends;
    }
}