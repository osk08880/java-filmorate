package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.genre.GenreDao;

import java.util.List;

@RestController
@RequestMapping("/genres")
@Slf4j
public class GenreController {
    private final GenreDao genreDao;

    public GenreController(GenreDao genreDao) {
        this.genreDao = genreDao;
    }

    @GetMapping
    public List<Genre> getAllGenres() {
        log.debug("Получен запрос GET /genres");
        return genreDao.findAll();
    }

    @GetMapping("/{id}")
    public Genre getGenreById(@PathVariable Long id) {
        log.debug("Получен запрос GET /genres/{}", id);
        return genreDao.findById(id)
                .orElseThrow(() -> new NotFoundException("Жанр с id " + id + " не найден"));
    }
}
