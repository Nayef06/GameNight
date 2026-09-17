# GameNight

GameNight is a deliberately small semester-project prototype. Users manually list the games they own, create or join groups, and see the games owned by every member of a group.

This repository implements only that flow. It has no external game APIs, recommendations, voting, chat, or other social features.

## Project layout

- `frontend/` — Next.js and TypeScript UI
- `backend/` — Spring Boot REST API
- `docker-compose.yml` — local PostgreSQL only

## Prerequisites

- Java 21 or newer
- Maven 3.9+
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
   npm install
   npm run dev
   ```

4. Open `http://localhost:3000`.

To demonstrate the full flow, create two accounts in separate browser profiles, add at least one identical game title to both libraries, then create a group with one account and join it with the other using the six-character code.

## Configuration

The default values match `docker-compose.yml` and are suitable for local development.

| Variable | Default | Purpose |
| --- | --- | --- |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/gamenight` | JDBC connection URL |
| `DATABASE_USERNAME` | `gamenight` | Database username |
| `DATABASE_PASSWORD` | `gamenight` | Database password |
| `JWT_SECRET` | development-only value | Base64-encoded JWT signing key |
| `NEXT_PUBLIC_API_URL` | `http://localhost:8080/api` | API base URL used by the UI |

Use a new Base64-encoded secret of at least 32 bytes outside local development.

## API

Authentication endpoints return a JWT. Send it to every other endpoint as `Authorization: Bearer <token>`.

| Method | Path | Body |
| --- | --- | --- |
| POST | `/api/auth/register` | `{ "username": "alex", "password": "secret1" }` |
| POST | `/api/auth/login` | `{ "username": "alex", "password": "secret1" }` |
| GET | `/api/library` | — |
| POST | `/api/library` | `{ "title": "Terraria" }` |
| DELETE | `/api/library/{gameId}` | — |
| GET | `/api/groups` | — |
| POST | `/api/groups` | `{ "name": "Friday Night Group" }` |
| POST | `/api/groups/join` | `{ "joinCode": "AB12CD" }` |
| GET | `/api/groups/{groupId}` | — |
| GET | `/api/groups/{groupId}/shared-games` | — |

Only members can view a group's details and shared games. Usernames, normalized game titles, game ownerships, join codes, and memberships are protected by database uniqueness constraints.

## Checks

```bash
cd backend && mvn test
cd frontend && npm run build
```
