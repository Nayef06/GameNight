# GameNight

GameNight is a deliberately small semester-project prototype. Users manually list the games they own with basic metadata, create or join groups, see and filter the games owned by every member of a group, save basic game preferences, and mark a shared game as recently played.

This repository implements only that flow. It has no external game APIs, voting, sessions, chat, or other social features.

## Project layout

- `frontend/` — Next.js and TypeScript UI
- `backend/` — Spring Boot REST API
- `docker-compose.yml` — local PostgreSQL only

## Tech stack

- Java 21, Spring Boot, Spring Security, Spring Data JPA, and JWT authentication
- PostgreSQL 16
- Next.js 16, React 19, and TypeScript

## Requirements

- Java 21 or newer
- Maven 3.6.3+
- Node.js 20+
- Docker (for the included PostgreSQL setup), or a local PostgreSQL server

## Run locally

1. Start PostgreSQL from the repository root:

   ```bash
   docker compose up -d
   ```

2. Start the API:

   ```bash
   cd backend
   mvn spring-boot:run
   ```

   The API runs at `http://localhost:8080`. JPA creates/updates the small development schema automatically.

3. In another terminal, start the UI:

   ```bash
   cd frontend
   npm ci
   npm run dev
   ```

4. Open `http://localhost:3000`.

PostgreSQL data is stored in the named Docker volume `gamenight_postgres_data`, so it remains available after the containers and applications stop. `docker compose down` preserves it; `docker compose down -v` removes it.

## Environment variables

The default values match `docker-compose.yml` and are suitable for local development.

| Variable | Default | Purpose |
| --- | --- | --- |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/gamenight` | JDBC connection URL |
| `DATABASE_USERNAME` | `gamenight` | Database username |
| `DATABASE_PASSWORD` | `gamenight` | Database password |
| `JWT_SECRET` | development-only value | Base64-encoded JWT signing key |
| `NEXT_PUBLIC_API_URL` | `http://localhost:8080/api` | API base URL used by the UI |

Use a new Base64-encoded secret of at least 32 bytes outside local development.

## Demo walkthrough

Use two browser profiles (or a normal and private window) so both users can stay logged in.

1. Register `Nayef` with any password of at least six characters. Usernames are stored in lowercase, so the UI will show `nayef`.
2. Add `Terraria`, `Minecraft`, and `Lethal Company` to Nayef's library, including a genre, multiplayer support, and maximum player count for each game.
3. Create a group named `Friday Night Group` and copy its six-character join code.
4. In the other browser profile, register `Alex` and add `Terraria`, `Lethal Company`, and `Valorant`.
5. Open **My Groups**, join with the copied code, and open **Friday Night Group**.
6. Confirm both `alex` and `nayef` are listed under **Members**.
7. Confirm **Shared Games** lists only `Lethal Company` and `Terraria`, then try the genre, multiplayer, and minimum-player filters.
8. Clear the filters and confirm both shared games return.
9. Choose one of the shared games and confirm it appears under **Recently Chosen** and remains in the shared-game list with a small ranking penalty.
10. Refresh the page. The members, shared games, and recent selection should still be present because they are stored in PostgreSQL.

## API

Authentication endpoints return a JWT. Send it to every other endpoint as `Authorization: Bearer <token>`.

| Method | Path | Body |
| --- | --- | --- |
| POST | `/api/auth/register` | `{ "username": "alex", "password": "secret1" }` |
| POST | `/api/auth/login` | `{ "username": "alex", "password": "secret1" }` |
| GET | `/api/library` | — |
| POST | `/api/library` | `{ "title": "Terraria", "genre": "Survival", "multiplayerSupport": true, "maxPlayers": 8 }` |
| DELETE | `/api/library/{gameId}` | — |
| GET | `/api/preferences` | — |
| PUT | `/api/preferences` | `{ "preferredGenres": ["Survival", "Shooter"], "multiplayerPreference": "MULTIPLAYER", "preferredPlayerCount": 4 }` |
| GET | `/api/groups` | — |
| POST | `/api/groups` | `{ "name": "Friday Night Group" }` |
| POST | `/api/groups/join` | `{ "joinCode": "AB12CD" }` |
| GET | `/api/groups/{groupId}` | — |
| DELETE | `/api/groups/{groupId}/members/{userId}` | — |
| POST | `/api/groups/{groupId}/leave` | — |
| GET | `/api/groups/{groupId}/shared-games` | Optional query parameters: `genre`, `multiplayerSupport`, `minPlayers` |
| POST | `/api/groups/{groupId}/selections` | `{ "gameId": 12 }` |
| GET | `/api/groups/{groupId}/selections/recent` | — |

Only members can view a group's details, shared games, and recent selections, or mark a shared game as played. The three most recent selections receive a two-point ranking penalty without being removed. Only the group creator can remove another member, normal members can leave, and the creator cannot leave their own group. Usernames, normalized game titles, game ownerships, join codes, and memberships are protected by database uniqueness constraints.

On startup, JPA adds the metadata and preference fields when upgrading an existing database. Existing games receive the safe defaults `Other`, `false`, and `1`; users receive no preferred genres, `NO_PREFERENCE`, and no preferred player count.

## Checks

```bash
cd backend && mvn test
cd frontend && npm run build
```
