/**
 * ai.js — Компьютерный противник
 * Автор: Зубков Владимир (аналитик / разработчик ИИ)
 * Review: Хусейнов А.
 *
 * Алгоритм: Минимакс с альфа-бета отсечением
 *   - Игрок 2 (ИИ) максимизирует оценочную функцию
 *   - Игрок 1 (человек) минимизирует
 *   - Бонусный ход сохраняет очерёдность (не переключает игрока)
 */

class BantumiAI {
  constructor(difficulty = 'medium') {
    // Глубина поиска зависит от уровня сложности
    const depths = { easy: 2, medium: 5, hard: 9 };
    this.depth = depths[difficulty] ?? 5;
    this.nodesEvaluated = 0;
  }

  // Эвристическая оценка позиции (положительная = выгодна ИИ)
  evaluate(game) {
    if (game.gameOver) {
      if (game.winner === 2)  return  10000;
      if (game.winner === 1)  return -10000;
      return 0; // Ничья
    }

    const kalahDiff = game.board[13] - game.board[6];

    // Бонус за бонусные ходы: лунки, которые закончатся ровно в Калах
    let bonusMovesP2 = 0;
    let bonusMovesP1 = 0;
    game.getPits(2).forEach(i => {
      const dist = (13 - i); // Расстояние от лунки до Калаха P2
      if (game.board[i] === dist) bonusMovesP2++;
    });
    game.getPits(1).forEach(i => {
      const dist = (6 - i);
      if (game.board[i] === dist) bonusMovesP1++;
    });

    // Бонус за потенциальные захваты
    let captureP2 = 0;
    let captureP1 = 0;
    game.getPits(2).forEach(i => {
      const landIndex = (i + game.board[i]) % 14;
      if (game.board[i] > 0 && game.ownsPit(2, landIndex) &&
          game.board[landIndex] === 0 && game.board[game.opposite(landIndex)] > 0) {
        captureP2 += game.board[game.opposite(landIndex)];
      }
    });
    game.getPits(1).forEach(i => {
      const stones = game.board[i];
      if (stones === 0) return;
      // Упрощённая проверка: пропускаем Калах противника при подсчёте
      let target = i + stones;
      if (target >= 6) target++; // Пропускаем Калах P2 (индекс 13 не учтён точно, но приближение)
      target = target % 14;
      if (game.ownsPit(1, target) && game.board[target] === 0 &&
          game.board[game.opposite(target)] > 0) {
        captureP1 += game.board[game.opposite(target)];
      }
    });

    return (
      kalahDiff * 3 +
      (bonusMovesP2 - bonusMovesP1) * 2 +
      (captureP2 - captureP1) * 1.5
    );
  }

  // Рекурсивный минимакс с альфа-бета отсечением
  minimax(game, depth, alpha, beta, maximizing) {
    this.nodesEvaluated++;

    if (depth === 0 || game.gameOver) {
      return this.evaluate(game);
    }

    const player = maximizing ? 2 : 1;
    const moves = game.getAvailableMoves(player);

    if (moves.length === 0) {
      return this.evaluate(game);
    }

    if (maximizing) {
      let maxEval = -Infinity;
      for (const move of moves) {
        const copy = game.clone();
        const result = copy.makeMove(move);
        // При бонусном ходе очерёдность не меняется
        const nextMaximizing = result.bonusTurn ? true : false;
        const score = this.minimax(copy, depth - 1, alpha, beta, nextMaximizing);
        maxEval = Math.max(maxEval, score);
        alpha = Math.max(alpha, score);
        if (beta <= alpha) break; // Бета-отсечение
      }
      return maxEval;
    } else {
      let minEval = Infinity;
      for (const move of moves) {
        const copy = game.clone();
        const result = copy.makeMove(move);
        const nextMaximizing = result.bonusTurn ? false : true;
        const score = this.minimax(copy, depth - 1, alpha, beta, nextMaximizing);
        minEval = Math.min(minEval, score);
        beta = Math.min(beta, score);
        if (beta <= alpha) break; // Альфа-отсечение
      }
      return minEval;
    }
  }

  // Выбрать лучший ход для ИИ (Игрок 2)
  getBestMove(game) {
    this.nodesEvaluated = 0;
    const moves = game.getAvailableMoves(2);
    if (moves.length === 0) return null;

    let bestMove = moves[0];
    let bestScore = -Infinity;

    for (const move of moves) {
      const copy = game.clone();
      const result = copy.makeMove(move);
      const nextMaximizing = result.bonusTurn ? true : false;
      const score = this.minimax(copy, this.depth - 1, -Infinity, Infinity, nextMaximizing);
      if (score > bestScore) {
        bestScore = score;
        bestMove = move;
      }
    }

    return bestMove;
  }
}
