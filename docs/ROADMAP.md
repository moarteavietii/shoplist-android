# Roadmap: shoplist-android

Ordered, high-level plan for the Android app that replaces the shoppinglist web frontend. Each item becomes one or more OpenSpec changes when work starts. This file tracks *what* and *why*; specs under `../openspec/specs` track the *contracts*.

## 1. App scaffold migration ✅ (done — `scaffold-compose-foundation`, archived 2026-10-22)

Replace the XML/AppCompat scaffold with the real foundation:

- Jetpack Compose with a **custom design language** (no Material look — port the web app's chromeless, borderless, LED-dot aesthetic; Material 3 used only as an unthemed component toolkit where convenient)
- Hilt (dependency injection), Retrofit + OkHttp + kotlinx.serialization (networking)
- Cloudflare Access auth: service-token headers on every request, encrypted storage, QR/manual token entry screen, 401/403 → re-entry flow (ported from the `helloworldcosmin` testbed). **Dev bypass:** the local dev backend sits outside Cloudflare Access, so auth must be skippable per build (e.g. no-op interceptor in debug builds) — design the auth layer so this is a build-time switch, not scattered conditionals
- Base URL: production `https://shoppinglist.pascariu.eu`; dev builds point at the workstation over LAN (`http://192.168.0.141:3000`, IP believed to be DHCP-reserved — verify; fallback alternative: Bonjour hostname `http://<macname>.local:3000`), health-check call

## 2. Data layer

- DTOs mirroring `packages/shared` in the web repo (items, shops, events)
- ItemsRepository + ShopsRepository with in-memory cache (no Room yet)
- `X-Device-Id` interceptor on mutations only
- Kotlin port of fractional indexing (`generateKeyBetween`) with unit tests

## 3. Realtime sync (SSE)

- OkHttp EventSource for items + shops streams, with `CF-Access-*` headers on connect
- Reconnect → full refetch contract (no replay on the server)
- Cache merge rules: idempotent `item:created`, diff-checked `item:updated`, filter `item:deleted`, re-sorted shops
- **Lifecycle-aware connections:** stop SSE (and any in-flight polling) when the app is backgrounded, resume + resync on foreground — power consumption is a hard requirement

## 4. List screen MVP

- Items list, check-off with optimistic update + rollback, instant undo (50 entries / 30-min TTL, device-local)
- Quick add from empty search

## 5. Views (shops) + widget stack

- View drawer (per-shop views, Global first), add/rename/delete shops
- Sort modes: Manual / A→Z / Frequency; show/hide checked; expandable fuzzy search
- LED status dots, unchecked-count badges

## 6. Feature parity

- Edit/delete item modals, multi-shop visibility, quantities
- Temporary items + 24 h expiry
- Change toasts (remote changes, own-device suppression, toggleable)
- Pull-to-refresh: **dropped** — SSE + refetch-on-reconnect is the freshness mechanism; revisit only if realtime proves unreliable

## 7. Themes

- Rethink the web app's 14 themes: pick a small curated set (several are too similar to be worth keeping)
- Port palettes as Compose color schemes; WCAG AA contrast as unit tests
- Exact selection to be discussed when this item starts

## 8. Offline support

- Local persistence (Room) + mutation queue so the app works without connectivity
- Sync strategy: reconcile queued mutations with server state and SSE on reconnect
- Conflicts: server remains source of truth; last-write-wins or per-field merge, decided at spec time
- Depends on: data layer (2), realtime sync (3)
