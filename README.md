# GameNight

GameNight is a deliberately small semester-project prototype. Users select the games they own from a built-in catalog of real titles, create or join groups, see and filter the games owned by every member of a group, save tag preferences, mark a shared game as recently played, and create a GameNight session with 2–5 shared candidate games. Catalog games can have multiple tags.

This repository implements only that flow. It has no external game APIs, voting, chat, or other social features.

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
2. Select `Terraria`, `Minecraft`, and `Lethal Company` from the catalog for Nayef's library. Their tags, multiplayer support, and maximum player count are provided by the catalog.
3. Create a group named `Friday Night Group` and copy its six-character join code.
4. In the other browser profile, register `Alex` and add `Terraria`, `Lethal Company`, and `Valorant`.
5. Open **My Groups**, join with the copied code, and open **Friday Night Group**.
6. Confirm both `alex` and `nayef` are listed under **Members**.
7. Confirm **Shared Games** lists only `Lethal Company` and `Terraria`, then select multiple tags to match all of them and try the multiplayer and minimum-player filters.
8. Clear the filters and confirm both shared games return.
9. Choose one of the shared games and confirm it appears under **Recently Chosen** and remains in the shared-game list with a small ranking penalty.
10. Create a GameNight session with both shared games, then open its detail page to see the candidates. A second open session cannot be created for the group.
11. Refresh the page. The members, shared games, recent selection, and session should still be present because they are stored in PostgreSQL.

## API

Authentication endpoints return a JWT. Send it to every other endpoint as `Authorization: Bearer <token>`.

| Method | Path | Body |
| --- | --- | --- |
| POST | `/api/auth/register` | `{ "username": "alex", "password": "secret1" }` |
| POST | `/api/auth/login` | `{ "username": "alex", "password": "secret1" }` |
| GET | `/api/games` | Available catalog games with tags and player metadata |
| GET | `/api/games/tags` | Available tags |
| GET | `/api/library` | — |
| POST | `/api/library` | `{ "gameId": 12 }` where the ID comes from `/api/games` |
| DELETE | `/api/library/{gameId}` | — |
| GET | `/api/preferences` | — |
| PUT | `/api/preferences` | `{ "preferredTags": ["Survival", "Co-op"], "multiplayerPreference": "MULTIPLAYER", "preferredPlayerCount": 4 }` |
| GET | `/api/groups` | — |
| POST | `/api/groups` | `{ "name": "Friday Night Group" }` |
| POST | `/api/groups/join` | `{ "joinCode": "AB12CD" }` |
| GET | `/api/groups/{groupId}` | — |
| DELETE | `/api/groups/{groupId}/members/{userId}` | — |
| POST | `/api/groups/{groupId}/leave` | — |
| GET | `/api/groups/{groupId}/shared-games` | Optional query parameters: repeated `tags` (all must match), `multiplayerSupport`, `minPlayers` |
| POST | `/api/groups/{groupId}/selections` | `{ "gameId": 12 }` |
| GET | `/api/groups/{groupId}/selections/recent` | — |
| POST | `/api/groups/{groupId}/sessions` | `{ "gameIds": [1, 3] }` |
| GET | `/api/groups/{groupId}/sessions/active` | —; returns 204 if none is open |
| GET | `/api/groups/{groupId}/sessions/{sessionId}` | — |

Only members can view a group's details, shared games, and recent selections, or mark a shared game as played. Each matching preferred tag adds one point per member, as do a matching multiplayer preference and support for their preferred player count. The three most recent selections receive a two-point ranking penalty without being removed. Shared games appear in score order, with tied top scores highlighted in green. Only the group creator can remove another member, normal members can leave, and the creator cannot leave their own group. Usernames, normalized game titles, game ownerships, join codes, and memberships are protected by database uniqueness constraints.

Any group member can create a session with 2–5 distinct games currently owned by every member. A group can have one open session at a time. Sessions start as `OPEN`; closing and voting are not implemented yet.

On startup, JPA adds tag and session storage and the catalog is seeded. Existing games remain in users' libraries; catalog matches get catalog metadata, while older custom games remain visible with their previous genre shown as one tag. New library additions must come from the catalog. Existing user preferences remain stored and are exposed as preferred tags.

## Checks

```bash
cd backend && mvn test
cd frontend && npm run build
```
