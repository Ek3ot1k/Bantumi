/**
 * ui.js — Пользовательский интерфейс
 * Автор: Старостин Максим (frontend)
 * Review: Хусейнов А.
 *
 * Отвечает за:
 *   - Отрисовку игрового поля
 *   - Обработку кликов игрока
 *   - Анимации и визуальную обратную связь
 *   - Управление режимами (2 игрока / против ИИ)
 *   - Журнал ходов
 */

class BantumiUI {
  constructor() {
    this.game = new BantumiGame(4);
    this.ai = new BantumiAI('medium');
    this.mode = 'vs-ai'; // 'vs-ai' | 'two-players'
    this.aiPlayer = 2;
    this.isAiThinking = false;
    this.moveCount = 0;

    this._cacheDOM();
    this._bindEvents();
    this._render();
  }

  // Кэшируем ссылки на DOM-элементы
  _cacheDOM() {
    this.dom = {
      rowP1:        document.getElementById('row-p1'),
      rowP2:        document.getElementById('row-p2'),
      kalahP1:      document.getElementById('kalah-p1'),
      kalahP2:      document.getElementById('kalah-p2'),
      kalahP1Count: document.getElementById('kalah-p1-count'),
      kalahP2Count: document.getElementById('kalah-p2-count'),
      indicator1:   document.getElementById('indicator-p1'),
      indicator2:   document.getElementById('indicator-p2'),
      statusBar:    document.getElementById('status-bar'),
      scoreP1:      document.getElementById('score-p1'),
      scoreP2:      document.getElementById('score-p2'),
      btnNew:       document.getElementById('btn-new'),
      btnUndo:      document.getElementById('btn-undo'),
      modeSelect:   document.getElementById('mode-select'),
      diffSelect:   document.getElementById('diff-select'),
      stonesRange:  document.getElementById('stones-range'),
      stonesVal:    document.getElementById('stones-val'),
      moveLog:      document.getElementById('move-log'),
      overlay:      document.getElementById('overlay'),
      overlayResult:document.getElementById('overlay-result'),
      overlayScores:document.getElementById('overlay-scores'),
      btnPlayAgain: document.getElementById('btn-play-again'),
      aiThinking:   document.getElementById('ai-thinking'),
      rulesToggle:  document.getElementById('rules-toggle'),
      rulesContent: document.getElementById('rules-content'),
    };
  }

  _bindEvents() {
    this.dom.btnNew.addEventListener('click', () => this._newGame());
    this.dom.btnUndo.addEventListener('click', () => this._undo());
    this.dom.btnPlayAgain.addEventListener('click', () => {
      this.dom.overlay.classList.remove('visible');
      this._newGame();
    });
    this.dom.modeSelect.addEventListener('change', () => {
      this.mode = this.dom.modeSelect.value;
      this._newGame();
    });
    this.dom.diffSelect.addEventListener('change', () => {
      this.ai = new BantumiAI(this.dom.diffSelect.value);
    });
    this.dom.stonesRange.addEventListener('input', () => {
      this.dom.stonesVal.textContent = this.dom.stonesRange.value;
    });
    this.dom.stonesRange.addEventListener('change', () => this._newGame());
    this.dom.rulesToggle.addEventListener('click', () => {
      this.dom.rulesToggle.classList.toggle('open');
      this.dom.rulesContent.classList.toggle('open');
    });
  }

  _newGame() {
    const stones = parseInt(this.dom.stonesRange.value, 10);
    this.game = new BantumiGame(stones);
    this.ai = new BantumiAI(this.dom.diffSelect.value);
    this.isAiThinking = false;
    this.moveCount = 0;
    this.dom.moveLog.innerHTML = '';
    this.dom.aiThinking.classList.remove('visible');
    this._render();

    // Если ИИ ходит первым (режим vs-ai и aiPlayer === 1)
    if (this.mode === 'vs-ai' && this.aiPlayer === 1) {
      this._scheduleAiMove();
    }
  }

  // Основная отрисовка доски
  _render() {
    this._renderRow(this.dom.rowP1, this.game.getPits(1));
    this._renderRow(this.dom.rowP2, this.game.getPits(2));
    this._renderKalah();
    this._renderStatus();
    this._renderScores();
    this.dom.btnUndo.disabled = this.game.history.length === 0 || this.game.gameOver;
  }

  _renderRow(container, pits) {
    container.innerHTML = '';
    pits.forEach(idx => {
      const pit = this._createPitEl(idx);
      container.appendChild(pit);
    });
  }

  _createPitEl(idx) {
    const div = document.createElement('div');
    div.className = 'pit';
    div.dataset.idx = idx;

    const count = this.game.board[idx];
    const player = idx <= 5 ? 1 : 2;
    const isMyTurn = this.game.currentPlayer === player;
    const clickable = isMyTurn && count > 0 && !this.game.gameOver && !this.isAiThinking;
    const isHumanTurn = this.mode === 'two-players' || this.game.currentPlayer !== this.aiPlayer;

    if (!clickable || !isHumanTurn) {
      div.classList.add('disabled');
      if (count === 0) div.classList.add('empty');
    } else {
      div.addEventListener('click', () => this._onPitClick(idx));
    }

    // Отметки последнего приземления и захвата
    if (idx === this.game.lastLandedPit) div.classList.add('last-landed');
    if (idx === this.game.capturedFrom)   div.classList.add('captured-highlight');

    // Визуальные камни (до 12 точек, далее только число)
    const stoneCount = document.createElement('div');
    stoneCount.className = 'stone-count';
    stoneCount.textContent = count;
    div.appendChild(stoneCount);

    if (count > 0 && count <= 12) {
      const visual = document.createElement('div');
      visual.className = 'stones-visual';
      for (let i = 0; i < Math.min(count, 12); i++) {
        const dot = document.createElement('div');
        dot.className = 'stone-dot';
        visual.appendChild(dot);
      }
      div.appendChild(visual);
    }

    return div;
  }

  _renderKalah() {
    this.dom.kalahP1Count.textContent = this.game.board[6];
    this.dom.kalahP2Count.textContent = this.game.board[13];

    // Индикатор хода
    const p1Active = this.game.currentPlayer === 1 && !this.game.gameOver;
    const p2Active = this.game.currentPlayer === 2 && !this.game.gameOver;
    this.dom.indicator1.classList.toggle('active', p1Active);
    this.dom.indicator2.classList.toggle('active', p2Active);
  }

  _renderStatus() {
    const bar = this.dom.statusBar;
    if (this.game.gameOver) {
      bar.className = 'gameover';
      if (this.game.winner === 0) {
        bar.textContent = 'Ничья!';
      } else {
        const name = this._playerName(this.game.winner);
        bar.textContent = `${name} победил!`;
      }
    } else if (this.isAiThinking) {
      bar.className = 'p2-turn';
      bar.textContent = 'ИИ думает...';
    } else {
      const p = this.game.currentPlayer;
      bar.className = p === 1 ? 'p1-turn' : 'p2-turn';
      bar.textContent = `Ход: ${this._playerName(p)}`;
    }
  }

  _renderScores() {
    this.dom.scoreP1.textContent = this.game.board[6];
    this.dom.scoreP2.textContent = this.game.board[13];
  }

  // Клик по лунке
  _onPitClick(idx) {
    if (this.isAiThinking || this.game.gameOver) return;
    if (this.mode === 'vs-ai' && this.game.currentPlayer === this.aiPlayer) return;

    const result = this.game.makeMove(idx);
    if (!result.valid) return;

    this.moveCount++;
    this._logMove(idx, result);
    this._animatePit(idx);
    this._render();

    if (result.gameOver) {
      setTimeout(() => this._showOverlay(), 600);
      return;
    }

    // Если теперь очередь ИИ
    if (this.mode === 'vs-ai' && this.game.currentPlayer === this.aiPlayer) {
      this._scheduleAiMove();
    }
  }

  // Запускаем ход ИИ с небольшой задержкой для UX
  _scheduleAiMove() {
    if (this.game.gameOver) return;
    this.isAiThinking = true;
    this.dom.aiThinking.classList.add('visible');
    this._renderStatus();

    // setTimeout позволяет браузеру обновить UI перед тяжёлым вычислением
    setTimeout(() => {
      const move = this.ai.getBestMove(this.game);
      if (move === null) {
        this.isAiThinking = false;
        this.dom.aiThinking.classList.remove('visible');
        return;
      }

      const result = this.game.makeMove(move);
      this.moveCount++;
      this._logMove(move, result, true);
      this._animatePit(move);
      this.isAiThinking = false;
      this.dom.aiThinking.classList.remove('visible');
      this._render();

      if (result.gameOver) {
        setTimeout(() => this._showOverlay(), 600);
        return;
      }

      // Если ИИ получил бонусный ход — ходит снова
      if (result.bonusTurn) {
        this._scheduleAiMove();
      }
    }, 400);
  }

  _animatePit(idx) {
    // Находим элемент лунки и добавляем CSS-анимацию
    const allPits = document.querySelectorAll('.pit');
    allPits.forEach(el => {
      if (parseInt(el.dataset.idx, 10) === idx) {
        el.classList.add('active-pit');
        setTimeout(() => el.classList.remove('active-pit'), 600);
      }
    });
  }

  _undo() {
    if (this.game.history.length === 0) return;
    const prev = this.game.history.pop();
    this.game.board = prev.board;
    this.game.currentPlayer = prev.player;
    this.game.gameOver = false;
    this.game.winner = null;
    this.game.lastLandedPit = null;
    this.game.capturedFrom = null;
    this._render();

    // Удаляем последнюю запись из журнала
    const log = this.dom.moveLog;
    if (log.firstChild) log.removeChild(log.firstChild);
  }

  _logMove(pitIdx, result, isAI = false) {
    const li = document.createElement('li');
    const player = isAI ? 'ИИ' : this._playerName(isAI ? this.aiPlayer : this.game.currentPlayer === 1 ? 2 : 1);
    const pitNum = this.game.getPits(1).includes(pitIdx)
      ? pitIdx + 1
      : pitIdx - 6; // Для P2: лунки 7-12 → показываем 1-6

    let text = `#${this.moveCount} ${player}: лунка ${pitNum}`;
    if (result.bonusTurn) { text += ' — бонусный ход!'; li.classList.add('bonus'); }
    if (result.captured)  { text += ' — захват!';       li.classList.add('capture'); }

    li.textContent = text;
    this.dom.moveLog.insertBefore(li, this.dom.moveLog.firstChild);
  }

  _showOverlay() {
    const { winner } = this.game;
    const scores = `Счёт: Игрок 1 — ${this.game.board[6]}, Игрок 2 — ${this.game.board[13]}`;
    let resultText = '';

    if (winner === 0) {
      resultText = 'Ничья! Оба игрока набрали поровну.';
    } else if (this.mode === 'vs-ai') {
      resultText = winner === 1 ? '🎉 Вы победили ИИ!' : '🤖 ИИ победил. Попробуйте ещё раз!';
    } else {
      resultText = `${this._playerName(winner)} победил!`;
    }

    this.dom.overlayResult.textContent = resultText;
    this.dom.overlayScores.textContent = scores;
    this.dom.overlay.classList.add('visible');
  }

  _playerName(player) {
    if (this.mode === 'vs-ai') {
      return player === 1 ? 'Игрок' : 'ИИ';
    }
    return player === 1 ? 'Игрок 1' : 'Игрок 2';
  }
}

// Запуск после загрузки DOM
document.addEventListener('DOMContentLoaded', () => {
  window.bantumiUI = new BantumiUI();
});
