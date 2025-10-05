package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;
import ru.yandex.practicum.filmorate.exception.NotFoundException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Profile("!test")
@Slf4j
public class UserService {
    private final UserStorage userStorage;

    @Autowired
    public UserService(@Qualifier("db") UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public void addFriend(Long userId, Long friendId) {
        log.debug("Добавление друга: пользователь id={}, друг id={}", userId, friendId);
        User user = getUserOrThrow(userId);
        User friend = getUserOrThrow(friendId);

        if (user.getFriends().containsKey(friendId)) {
            log.warn("Дружба между пользователями id={} и id={} уже существует", userId, friendId);
            throw new IllegalStateException("Пользователи уже являются друзьями");
        }

        user.getFriends().put(friendId, User.FriendshipStatus.НЕПОДТВЕРЖДЁННАЯ);
        userStorage.update(user);
        log.info("Пользователь id={} отправил запрос на дружбу пользователю id={}", userId, friendId);
    }

    public void confirmFriend(Long userId, Long friendId) {
        log.debug("Подтверждение дружбы: пользователь id={}, друг id={}", userId, friendId);
        User user = getUserOrThrow(userId);
        User friend = getUserOrThrow(friendId);

        if (!user.getFriends().containsKey(friendId) || user.getFriends().get(friendId) != User.FriendshipStatus.НЕПОДТВЕРЖДЁННАЯ) {
            log.warn("Запрос на дружбу между id={} и id={} не найден", userId, friendId);
            throw new IllegalStateException("Запрос на дружбу не существует");
        }

        user.getFriends().put(friendId, User.FriendshipStatus.ПОДТВЕРЖДЁННАЯ);
        friend.getFriends().put(userId, User.FriendshipStatus.ПОДТВЕРЖДЁННАЯ);
        userStorage.update(user);
        userStorage.update(friend);
        log.info("Дружба между пользователями id={} и id={} подтверждена", userId, friendId);
    }

    public void removeFriend(Long userId, Long friendId) {
        log.debug("Удаление друга: пользователь id={}, друг id={}", userId, friendId);
        User user = getUserOrThrow(userId);
        User friend = getUserOrThrow(friendId);

        if (!user.getFriends().containsKey(friendId)) {
            log.warn("Дружба между id={} и id={} не существует", userId, friendId);
            return;
        }

        user.getFriends().remove(friendId);
        if (friend.getFriends().containsKey(userId)) {
            friend.getFriends().remove(userId);
        }
        userStorage.update(user);
        userStorage.update(friend);
        log.info("Дружба между пользователями id={} и id={} разорвана", userId, friendId);
    }

    public List<User> getCommonFriends(Long userId, Long otherUserId) {
        log.debug("Получение общих друзей: пользователь id={}, другой пользователь id={}", userId, otherUserId);
        User user = getUserOrThrow(userId);
        User otherUser = getUserOrThrow(otherUserId);

        Set<Long> commonFriendIds = new HashSet<>(user.getFriends().keySet());
        commonFriendIds.retainAll(otherUser.getFriends().keySet());
        log.debug("Найдены общие друзья: {}", commonFriendIds);

        List<User> commonFriends = commonFriendIds.stream()
                .map(this::getUserOrThrow)
                .collect(Collectors.toList());
        log.info("Возвращён список общих друзей для пользователей id={} и id={}: {} друзей",
                userId, otherUserId, commonFriends.size());
        return commonFriends;
    }

    public List<User> getFriends(Long userId) {
        log.debug("Получение списка друзей пользователя id={}", userId);
        User user = getUserOrThrow(userId);

        List<User> friends = user.getFriends().entrySet().stream()
                .map(Map.Entry::getKey)
                .map(this::getUserOrThrow)
                .collect(Collectors.toList());
        log.info("Возвращён список друзей для пользователя id={}: {} друзей", userId, friends.size());
        return friends;
    }

    private User getUserOrThrow(Long userId) {
        try {
            return userStorage.findById(userId);
        } catch (NotFoundException e) {
            log.error("Пользователь с id={} не найден", userId);
            throw e;
        }
    }
}