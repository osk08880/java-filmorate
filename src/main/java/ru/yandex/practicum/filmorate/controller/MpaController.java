package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.mpa.MpaDao;

import java.util.List;

@RestController
@RequestMapping("/mpa")
@Slf4j
public class MpaController {
    private final MpaDao mpaDao;

    public MpaController(MpaDao mpaDao) {
        this.mpaDao = mpaDao;
    }

    @GetMapping
    public List<Mpa> getAllMpa() {
        log.debug("Получен запрос GET /mpa");
        return mpaDao.findAll();
    }

    @GetMapping("/{id}")
    public Mpa getMpaById(@PathVariable Long id) {
        log.debug("Получен запрос GET /mpa/{}", id);
        return mpaDao.findById(id)
                .orElseThrow(() -> new NotFoundException("Рейтинг MPA с id " + id + " не найден"));
    }
}
