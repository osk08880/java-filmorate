package ru.yandex.practicum.filmorate.storage.mpa;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.List;
import java.util.Optional;

@Component
@Profile("test")
@Slf4j
public class MpaDbStorage implements MpaDao {
    private final JdbcTemplate jdbcTemplate;

    public MpaDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Mpa> findAll() {
        String sql = "SELECT id, rating AS name FROM mpa_ratings ORDER BY id";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Mpa mpa = new Mpa();
            mpa.setId(rs.getLong("id"));
            mpa.setName(rs.getString("name"));
            return mpa;
        });
    }

    @Override
    public Optional<Mpa> findById(Long id) {
        String sql = "SELECT id, rating AS name FROM mpa_ratings WHERE id = ?";
        List<Mpa> result = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Mpa mpa = new Mpa();
            mpa.setId(rs.getLong("id"));
            mpa.setName(rs.getString("name"));
            return mpa;
        }, id);
        return result.stream().findFirst();
    }

    // ✅ Метод для проверки существования MPA по id
    public boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM mpa_ratings WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }
}
