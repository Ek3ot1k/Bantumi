# Бантуми (Kalah)

Игра для двух игроков. Бэкенд — Java (Spring Boot), фронтенд — Python (Tkinter).

## Запуск

### 1. Бэкенд (Java)
```bash
cd backend
mvn spring-boot:run
```
Сервер запускается на `http://localhost:8080`

### 2. Фронтенд (Python)
```bash
cd frontend
pip3 install -r requirements.txt
python3 main.py
```

> Сначала запустить бэкенд, потом фронтенд.

## API

| Метод | URL | Описание |
|-------|-----|---------|
| GET | `/api/v1/game/state` | Текущее состояние |
| POST | `/api/v1/game/new?stones=4` | Новая игра |
| POST | `/api/v1/game/move/{pit}` | Ход из лунки |
| POST | `/api/v1/game/undo` | Отменить ход |

## Команда
- **Хусейнов Амин** — тимлид, Java backend
- **Старостин Максим** — Python frontend
- **Зубков Владимир** — аналитик, тестирование, документация
