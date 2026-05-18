package com.bantumi.repository;

import com.bantumi.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Репозиторий для работы с партиями в БД.
 */
@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

    /**
     * Загружает самую последнюю партию по времени обновления.
     * Используется для получения текущей активной игры.
     */
    Optional<Game> findTopByOrderByUpdatedAtDesc();
}
