package com.bantumi.repository;

import com.bantumi.entity.Game;
import com.bantumi.entity.Move;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Репозиторий для работы с ходами в БД.
 */
@Repository
public interface MoveRepository extends JpaRepository<Move, Long> {

    /**
     * Находит последний ход в партии (по порядковому номеру хода).
     * Используется для функции undo.
     */
    Optional<Move> findTopByGameOrderByMoveNumberDesc(Game game);
}
