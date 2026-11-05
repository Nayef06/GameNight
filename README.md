# GameNight

GameNight is a deliberately small semester-project prototype. Users select the games they own from a built-in catalog of real titles, create or join groups, see and filter the games owned by every member of a group, save tag preferences, mark a shared game as recently played, and create a GameNight session with 2–5 shared candidate games. Members can vote for one candidate, change their vote while the session is open, and view vote counts. Catalog games can have multiple tags.

This repository implements that flow plus optional Steam library importing, a platform field on library ownership, and optional Discord webhook announcements when sessions start or close. Vote counts update automatically while a session is open, and members receive basic in-app notifications when sessions are created or closed.

It is initially a project for my software engineering class, but I also use a Java backend to gain more hands-on experience with the stack.

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
| `STEAM_API_KEY` | unset | Server-side Steam Web API key for owned-game importing |
| `NEXT_PUBLIC_API_URL` | `http://localhost:8080/api` | API base URL used by the UI |

Use a new Base64-encoded secret of at least 32 bytes outside local development.

To use Steam importing, obtain a Steam Web API key from [Steam's API key page](https://steamcommunity.com/dev/apikey), set `STEAM_API_KEY` in the backend environment, and restart the API. On **My Library**, enter your numeric SteamID64 and select **Save and Import**. Later, select **Sync Steam Library** to import newly discovered games. The Steam [GetOwnedGames API](https://partner.steamgames.com/doc/webapi/IPlayerService) returns games only when their ownership details are visible to the key holder. Private or otherwise inaccessible libraries cannot be imported. Syncing only adds games; it does not remove games that disappear from Steam. Manual library entries remain available.

Manual catalog additions include a **Platform** dropdown: Steam, Epic Games, Xbox, PlayStation, Nintendo Switch, or Other. Platform belongs to each user's ownership record; the same user can own a title on multiple platforms. Removing an entry removes only that platform. Steam sync adds Steam ownership while preserving other platforms. Shared games still match by title and count each member once, regardless of platform. Shared lists show the platforms and a note when they differ; cross-play compatibility is not verified or used in ranking.

On a group's existing page, the creator can save or remove a Discord channel webhook under **Discord Integration**. Only standard HTTPS webhook endpoints on `discord.com` or `discordapp.com` are accepted. The saved URL is a backend secret: API responses and the UI show only whether it is configured. Session creation posts a short announcement; closing posts the winner, tied games, or that no votes were submitted. Messages use Discord's [Execute Webhook endpoint](https://docs.discord.com/developers/resources/webhook#execute-webhook), with mentions disabled. Delivery occurs after the session transaction commits, with short timeouts and no redirects. Failures are logged without the URL or token and do not undo sessions, votes, or in-app notifications. No bot, OAuth, or additional platform APIs are involved.

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
10. Create a GameNight session with both shared games, then open its detail page and vote for one candidate. A second open session cannot be created for the group.
11. In the other browser profile, vote for a candidate. The first browser's counts update within about five seconds without a page refresh. Try changing a vote.
12. Open **Notifications** in the other browser profile to see the session creation message, then mark it as read.
13. As the group creator, select **Close Voting**. The final counts and winner (or tie) remain visible, and the group can create another session. Group members can see the closure message on **Notifications**.
14. Refresh the page. The members, shared games, recent selection, session, votes, and notifications should still be present because they are stored in PostgreSQL.

## API

Authentication endpoints return a JWT. Send it to every other endpoint as `Authorization: Bearer <token>`.

Steam endpoints: `GET /api/users/me/steam` reads the current user's saved SteamID64; `PUT /api/users/me/steam` accepts `{ "steamId": "7656119XXXXXXXXXX" }`; `POST /api/library/steam/sync` imports owned games and returns `imported` and `alreadyInLibrary` counts.

| Method | Path | Body |
| --- | --- | --- |
| POST | `/api/auth/register` | `{ "username": "alex", "password": "secret1" }` |
| POST | `/api/auth/login` | `{ "username": "alex", "password": "secret1" }` |
| GET | `/api/games` | Available catalog games with tags and player metadata |
| GET | `/api/games/tags` | Available tags |
| GET | `/api/library` | — |
| POST | `/api/library` | `{ "gameId": 12, "platform": "XBOX" }` where the ID comes from `/api/games`; omitted platform defaults to `OTHER` |
| DELETE | `/api/library/{gameId}?platform=XBOX` | —; removes only this platform (defaults to `OTHER`) |
| GET | `/api/preferences` | — |
| PUT | `/api/preferences` | `{ "preferredTags": ["Survival", "Co-op"], "multiplayerPreference": "MULTIPLAYER", "preferredPlayerCount": 4 }` |
| GET | `/api/groups` | — |
| POST | `/api/groups` | `{ "name": "Friday Night Group" }` |
| POST | `/api/groups/join` | `{ "joinCode": "AB12CD" }` |
| GET | `/api/groups/{groupId}` | — |
| GET | `/api/groups/{groupId}/statistics` | —; members only |
| PUT | `/api/groups/{groupId}/discord-webhook` | `{ "webhookUrl": "https://discord.com/api/webhooks/.../..." }`; creator only |
| DELETE | `/api/groups/{groupId}/discord-webhook` | —; creator only |
| DELETE | `/api/groups/{groupId}/members/{userId}` | — |
| POST | `/api/groups/{groupId}/leave` | — |
| GET | `/api/groups/{groupId}/shared-games` | Optional query parameters: repeated `tags` (all must match), `multiplayerSupport`, `minPlayers` |
| POST | `/api/groups/{groupId}/selections` | `{ "gameId": 12 }` |
| GET | `/api/groups/{groupId}/selections/recent` | — |
| POST | `/api/groups/{groupId}/sessions` | `{ "gameIds": [1, 3] }` |
| GET | `/api/groups/{groupId}/sessions/active` | —; returns 204 if none is open |
| GET | `/api/groups/{groupId}/sessions/{sessionId}` | — |
| POST | `/api/groups/{groupId}/sessions/{sessionId}/vote` | `{ "gameId": 12 }` |
| GET | `/api/groups/{groupId}/sessions/{sessionId}/results` | — |
| POST | `/api/groups/{groupId}/sessions/{sessionId}/close` | — |
| GET | `/api/notifications` | —; returns only the current user's notifications |
| PATCH | `/api/notifications/{id}/read` | —; marks one of the current user's notifications as read |

Only members can view a group's details, shared games, and recent selections, or mark a shared game as played. Each matching preferred tag adds one point per member, as do a matching multiplayer preference and support for their preferred player count. Games with reliable player-count metadata gain two points when their capacity accommodates the actual group size and lose two points otherwise. Missing or invalid counts and imported default metadata do not change that score. The three most recent selections receive a two-point ranking penalty without being removed. Shared games appear in score order, with tied top scores highlighted in green. Only the group creator can remove another member, normal members can leave, and the creator cannot leave their own group. Usernames, normalized game titles, game ownerships, join codes, and memberships are protected by database uniqueness constraints.

The group page shows completed sessions (status `CLOSED`), the total recorded game selections, and the most selected game with its count. Statistics use the full persisted selection history, with alphabetical ordering for ties; votes and voting winners are not selections. Empty groups show zero counts and “None yet.” Statistics are available only to current group members and refresh after a game is marked as played.

Any group member can create a session with 2–5 distinct games currently owned by every member. A group can have one open session at a time. Sessions start as `OPEN`. Each member has one vote per session and may change it while voting is open. Results include every candidate, sorted by vote count and then alphabetically. Only the group creator can close voting. Closed results remain available, and a new session can then be created. Voting does not update recently chosen games. The session page checks results every five seconds while voting is open and stops checking when voting closes or the page is left. Session creation notifies other current group members; closure notifies all current group members. Notifications are stored in PostgreSQL and can be marked as read.

On startup, JPA adds tag, session, vote, and notification storage and the catalog is seeded. Existing games remain in users' libraries; catalog matches get catalog metadata, while older custom games remain visible with their previous genre shown as one tag. New library additions must come from the catalog. Existing user preferences remain stored and are exposed as preferred tags.

The ownership-platform migration runs automatically with Flyway before JPA updates the schema. It preserves existing ownership IDs and replaces uniqueness by user/game with uniqueness by user/game/platform. The old schema did not retain per-ownership import provenance, so legacy entries start as `Other`. Their next successful Steam sync confirms matching legacy entries as `Steam`; unmatched entries stay `Other`. New manual entries retain the selected platform, and all new Steam imports are labeled `Steam`.

## Checks

```bash
cd backend && mvn test
cd frontend && npm test && npm run build
```
