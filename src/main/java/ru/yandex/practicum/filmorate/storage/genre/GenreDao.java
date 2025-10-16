package ru.yandex.practicum.filmorate.storage.genre;

import ru.yandex.practicum.filmorate.model.Genre;
import java.util.List;
import java.util.Optional;

public interface GenreDao {
    List<Genre> findAll();

    Optional<Genre> findById(Long id);

    boolean existsById(Long id);
}
