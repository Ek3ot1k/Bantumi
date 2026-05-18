package com.bantumi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA-сущность для хранения состояния партии в БД.
 * board[14]: 0-5 — лунки П1, 6 — Калах П1, 7-12 — лунки П2, 13 — Калах П2
 */
@Entity
@Table(name = "games")
public class Game {

    /** Первичный ключ, генерируется автоматически */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Состояние доски — 14 чисел через запятую */
    @Column(name = "board_state", nullable = false, length = 100)
    private String boardState;

    /** Текущий игрок (1 или 2) */
    @Column(name = "current_player", nullable = false)
    private int currentPlayer;

    /** Флаг окончания игры */
    @Column(name = "game_over", nullable = false)
    private boolean gameOver;

    /** Победитель: 1, 2, 0 = ничья, null = игра не окончена */
    @Column(name = "winner")
    private Integer winner;

    /** Количество сделанных ходов */
    @Column(name = "move_count", nullable = false)
    private int moveCount;

    /** Индекс лунки, куда упал последний камень (для подсветки в UI) */
    @Column(name = "last_landed_pit")
    private Integer lastLandedPit;

    /** Количество камней в каждой лунке при старте */
    @Column(name = "stones_per_pit", nullable = false)
    private int stonesPerPit;

    /** Режим игры: "PVP" (два человека) или "PVE" (против AI) */
    @Column(name = "game_mode", nullable = false, length = 10)
    private String gameMode;

    /** Дата и время создания партии */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Дата и время последнего обновления партии */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Список ходов этой партии (ленивая загрузка) */
    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Move> moves = new ArrayList<>();

    public Game() {}

    /** Устанавливаем временные метки при создании */
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /** Обновляем временную метку при каждом сохранении */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // --- Геттеры и сеттеры ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBoardState() { return boardState; }
    public void setBoardState(String boardState) { this.boardState = boardState; }

    public int getCurrentPlayer() { return currentPlayer; }
    public void setCurrentPlayer(int currentPlayer) { this.currentPlayer = currentPlayer; }

    public boolean isGameOver() { return gameOver; }
    public void setGameOver(boolean gameOver) { this.gameOver = gameOver; }

    public Integer getWinner() { return winner; }
    public void setWinner(Integer winner) { this.winner = winner; }

    public int getMoveCount() { return moveCount; }
    public void setMoveCount(int moveCount) { this.moveCount = moveCount; }

    public Integer getLastLandedPit() { return lastLandedPit; }
    public void setLastLandedPit(Integer lastLandedPit) { this.lastLandedPit = lastLandedPit; }

    public int getStonesPerPit() { return stonesPerPit; }
    public void setStonesPerPit(int stonesPerPit) { this.stonesPerPit = stonesPerPit; }

    public String getGameMode() { return gameMode; }
    public void setGameMode(String gameMode) { this.gameMode = gameMode; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<Move> getMoves() { return moves; }
    public void setMoves(List<Move> moves) { this.moves = moves; }
}
