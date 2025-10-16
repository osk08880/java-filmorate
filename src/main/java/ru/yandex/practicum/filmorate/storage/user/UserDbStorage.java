package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component("db")
@Profile("!test")
@Slf4j
public class UserDbStorage implements UserStorage {
    private final JdbcTemplate jdbcTemplate;

    public UserDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public User create(User user) {
        log.debug("Создание пользователя в БД: {}", user);
        validateUser(user);
        checkDuplicateEmail(user.getEmail());

        String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getLogin());
            ps.setString(3, user.getName() != null ? user.getName() : user.getLogin());
            ps.setDate(4, Date.valueOf(user.getBirthday()));
            return ps;
        }, keyHolder);
        Long id = keyHolder.getKey().longValue();
        user.setId(id);
        saveFriends(id, user.getFriends());
        log.info("Пользователь успешно создан в БД: id={}", id);
        return findById(id);
    }

    @Override
    public User update(User newUser) {
        log.debug("Обновление пользователя в БД: {}", newUser);
        if (newUser.getId() == null) {
            throw new ConditionsNotMetException("Id должен быть указан");
        }
        findById(newUser.getId());
        checkDuplicateEmail(newUser.getEmail(), newUser.getId());

        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                newUser.getEmail(),
                newUser.getLogin(),
                newUser.getName() != null && !newUser.getName().isBlank() ? newUser.getName() : newUser.getLogin(),
                Date.valueOf(newUser.getBirthday()),
                newUser.getId());
        jdbcTemplate.update("DELETE FROM friendships WHERE user_id = ?", newUser.getId());
        saveFriends(newUser.getId(), newUser.getFriends());
        log.info("Пользователь успешно обновлен в БД: id={}", newUser.getId());
        return findById(newUser.getId());
    }

    @Override
    public Collection<User> findAll() {
        log.debug("Получение всех пользователей из БД");
        String sql = "SELECT u.id, u.email, u.login, u.name, u.birthday FROM users u";
        List<User> users = jdbcTemplate.query(sql, userRowMapper());
        loadAllFriends(users);
        log.info("Возвращено {} пользователей из БД", users.size());
        return users;
    }

    @Override
    public User findById(Long id) {
        log.debug("Поиск пользователя в БД по id={}", id);
        String sql = "SELECT u.id, u.email, u.login, u.name, u.birthday FROM users u WHERE u.id = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper(), id);
            if (user != null) {
                loadFriends(user);
                log.info("Пользователь найден в БД: id={}", id);
                return user;
            }
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            log.warn("Пользователь с id={} не найден в БД", id);
            throw new NotFoundException("Пользователь с id " + id + " не найден");
        }
        throw new NotFoundException("Пользователь с id " + id + " не найден");
    }

    private void validateUser(User user) {
        if (user.getBirthday().isAfter(LocalDate.now())) {
            throw new ConditionsNotMetException("Дата рождения не может быть в будущем");
        }
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }

    private void checkDuplicateEmail(String email) {
        checkDuplicateEmail(email, null);
    }

    private void checkDuplicateEmail(String email, Long excludeId) {
        String sql = "SELECT COUNT(*) FROM users WHERE LOWER(email) = LOWER(?)";
        Object[] params;
        if (excludeId != null) {
            sql += " AND id != ?";
            params = new Object[]{email, excludeId};
        } else {
            params = new Object[]{email};
        }
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, params);
        if (count != null && count > 0) {
            throw new DuplicatedDataException("Этот имейл уже используется");
        }
    }

    private void saveFriends(Long userId, Map<Long, User.FriendshipStatus> friends) {
        if (friends == null || friends.isEmpty()) {
            return;
        }

        List<Object[]> batchArgs = new ArrayList<>();
        for (Map.Entry<Long, User.FriendshipStatus> entry : friends.entrySet()) {
            if (entry.getValue() != null) {
                batchArgs.add(new Object[]{userId, entry.getKey(), entry.getValue().name()});
            }
        }

        if (batchArgs.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO friendships (user_id, friend_id, status) VALUES (?, ?, ?)";
        int[] updateCounts = jdbcTemplate.batchUpdate(sql, batchArgs);
        log.debug("Сохранено {} записей дружбы для пользователя id={}", updateCounts.length, userId);
    }

    private void loadFriends(User user) {
        String sql = "SELECT friend_id, status FROM friendships WHERE user_id = ?";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, user.getId());
        Map<Long, User.FriendshipStatus> friends = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long friendId = (Long) row.get("friend_id");
            String statusStr = (String) row.get("status");
            User.FriendshipStatus status = parseFriendshipStatus(statusStr);
            friends.put(friendId, status);
        }
        user.setFriends(friends);
    }

    private void loadAllFriends(List<User> users) {
        if (users.isEmpty()) return;

        String sql = "SELECT user_id, friend_id, status FROM friendships";
        List<Map<String, Object>> allRows = jdbcTemplate.queryForList(sql);

        Map<Long, Map<Long, User.FriendshipStatus>> allFriends = allRows.stream()
                .collect(Collectors.groupingBy(
                        row -> (Long) row.get("user_id"),
                        Collectors.toMap(
                                row -> (Long) row.get("friend_id"),
                                row -> parseFriendshipStatus((String) row.get("status"))
                        )
                ));

        for (User user : users) {
            Map<Long, User.FriendshipStatus> userFriends = allFriends.getOrDefault(user.getId(), new HashMap<>());
            user.setFriends(userFriends);
        }

        log.debug("Загружено друзей для {} пользователей из БД", users.size());
    }

    private User.FriendshipStatus parseFriendshipStatus(String statusStr) {
        if (statusStr == null) {
            return null;
        }
        statusStr = statusStr.trim().toUpperCase();
        switch (statusStr) {
            case "НЕПОДТВЕРЖДЁННАЯ":
                return User.FriendshipStatus.UNCONFIRMED;
            case "ПОДТВЕРЖДЁННАЯ":
                return User.FriendshipStatus.CONFIRMED;
            default:
                try {
                    return User.FriendshipStatus.valueOf(statusStr);
                } catch (IllegalArgumentException e) {
                    log.error("Неизвестный статус дружбы из БД: {}", statusStr);
                    throw new IllegalArgumentException("Неизвестный статус дружбы: " + statusStr);
                }
        }
    }

    private RowMapper<User> userRowMapper() {
        return (rs, rowNum) -> {
            User user = new User();
            user.setId(rs.getLong("id"));
            user.setEmail(rs.getString("email"));
            user.setLogin(rs.getString("login"));
            user.setName(rs.getString("name"));
            user.setBirthday(rs.getDate("birthday").toLocalDate());
            return user;
        };
    }
}