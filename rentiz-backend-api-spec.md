# Rentiz — REST API Data Contract & Architecture Specification

Reverse-engineered from the current implementation (TanStack Start + Postgres/Supabase) as a target contract for a **Spring Boot** rebuild.

Conventions used below:
- Base path: `/api/v1`
- All timestamps: ISO-8601 UTC strings (`2026-07-29T09:14:15.123Z`) → `OffsetDateTime`
- All ids: UUID v4 strings → `UUID` (except `properties.public_id`, an 8-char code)
- Auth: `Public` = no token; `Bearer` = `Authorization: Bearer <JWT>`; `Bearer+Admin` = JWT whose user holds role `admin`
- Errors (current UI only reads the message): `{ "error": "<message>" }` with HTTP 400/401/403/404/429/500. Special sentinel: `PHONE_REQUIRED` (see §2.6).

---

## 1. Data Models & Entities

### 1.1 Enums

| Enum | Values |
|---|---|
| `app_role` | `admin`, `user` |
| `bhk_type` | `1RK`, `1BHK`, `2BHK`, `3BHK`, `4BHK`, `4+BHK`, `PG` |
| `property_type_enum` | `Apartment`, `Independent House/Villa`, `Gated Community Villa`, `Gated Society` |
| `furnishing_type` | `Full`, `Semi`, `None` |
| `availability_bucket` | `Immediate`, `Within 15 Days`, `Within 30 Days`, `After 30 Days` |
| `property_age_bucket` | `<1 year`, `<3 years`, `<5 years`, `<10 years`, `10+ years` |
| `flag_reason` | `fake_listing`, `wrong_contact`, `already_rented`, `spam_or_scam`, `inappropriate`, `other` |
| `listed_by` (text) | `owner`, `broker` |
| `appeal_status` (text) | `none`, `pending`, `approved`, `rejected` |
| `preferred_tenants[]` (text[]) | `Family`, `Couple Friendly`, `Bachelor Male`, `Bachelor Female`, `Company` |
| `feedback_category` (not persisted) | `General feedback`, `Bug report`, `Feature request`, `Report a listing`, `Account help`, `Other` |

`city_id` is a client-side catalog (not a DB table), from `src/lib/cities.ts`:
`bengaluru`, `chennai`, `mumbai`, `delhi`, `hyderabad`, `pune` — each with `name`, `state`, `center{lat,lng}`, `zoom`, `tagline`.

### 1.2 `properties` (core entity)

PK `id`. FK `owner_id → auth.users.id` (nullable). Unique: `public_id`.

| Field | TS type | Null | DB default | Notes |
|---|---|---|---|---|
| `id` | `string` | no | `gen_random_uuid()` | PK |
| `public_id` | `string` | no | trigger `set_property_public_id` | 8 chars from alphabet `ABCDEFGHJKLMNPQRSTUVWXYZ23456789`, unique, searchable |
| `owner_id` | `string \| null` | yes | — | FK users |
| `city_id` | `string` | no | — | 1..40 chars |
| `title` | `string` | no | — | 6..120 chars |
| `description` | `string \| null` | yes | — | ≤2000 |
| `bhk_type` | `BhkType` | no | — | enum |
| `rent` | `number` (int) | no | — | 500..2_000_000 |
| `deposit` | `number` (int) | no | `0` | 0..20_000_000 |
| `is_lease` | `boolean` | no | `false` | |
| `property_type` | `PropertyType` | no | `Apartment` | |
| `furnishing` | `Furnishing` | no | `Semi` | |
| `parking_2w` | `boolean` | no | `false` | |
| `parking_4w` | `boolean` | no | `false` | |
| `built_up_area_sqft` | `number \| null` | yes | — | 50..50_000 |
| `property_age` | `PropertyAge \| null` | yes | — | |
| `bathrooms` | `number` (int) | no | `1` | 0..20 |
| `floor` | `number \| null` | yes | — | -2..200 |
| `total_floors` | `number \| null` | yes | — | 0..200 |
| `availability` | `Availability` | no | `Immediate` | |
| `available_from` | `string \| null` (date) | yes | — | `LocalDate` |
| `preferred_tenants` | `string[]` | no | `{}` | ≤4 items |
| `non_veg_allowed` | `boolean` | no | `false` | |
| `has_gym` | `boolean` | no | `false` | |
| `hosting_visit_next_2_days` | `boolean` | no | `false` | |
| `image_urls` | `string[]` | no | `{}` | 1..2 URLs on create |
| `area` | `string \| null` | yes | — | locality, ≤120 |
| `lat` | `number` (double) | no | — | -90..90 |
| `lng` | `number` (double) | no | — | -180..180 |
| `owner_name` | `string \| null` | yes | — | **PII — never in public reads** |
| `owner_whatsapp` | `string` | no | — | **PII**, exactly 10 digits |
| `owner_email` | `string \| null` | yes | — | **PII** |
| `flag_count` | `number` (int) | no | `0` | maintained by flag trigger |
| `suspended` | `boolean` | no | `false` | auto at ≥10 flags |
| `suspended_until` | `string \| null` | yes | — | now()+2 days on auto-suspend |
| `verified_by_admin` | `boolean` | no | `false` | immune to flags |
| `appeal_status` | `'none'\|'pending'\|'approved'\|'rejected'` | no | `none` | |
| `appeal_note` | `string \| null` | yes | — | |
| `archived` | `boolean` | no | `false` | owner soft-delete |
| `listed_by` | `'owner'\|'broker'` | no | `owner` | |
| `created_at` | `string` | no | `now()` | |
| `updated_at` | `string` | no | `now()` | touched by trigger |

**Public visibility rule (must be enforced server-side):**
`archived = false AND (suspended = false OR suspended_until < now())`

**PII rule:** `owner_name`, `owner_whatsapp`, `owner_email` MUST NOT appear in any public/authenticated read DTO. They are exposed only via §2.6 reveal endpoint and admin endpoints.

### 1.3 `profiles`

PK `id` = FK `auth.users.id`. No delete.

| Field | TS type | Null | Default |
|---|---|---|---|
| `id` | `string` | no | — |
| `full_name` | `string \| null` | yes | from signup metadata (`full_name`/`name`) |
| `phone` | `string \| null` | yes | 10 digits when set |
| `phone_verified` | `boolean` | no | `false` |
| `created_at` / `updated_at` | `string` | no | `now()` |

Auto-created by trigger on user signup. API DTO adds derived `email` (from JWT claims, not stored).

### 1.4 `user_roles`

PK `id`; unique `(user_id, role)`; FKs `user_id → users.id`, `promoted_by → users.id`.

| Field | TS type | Null |
|---|---|---|
| `id` | `string` | no |
| `user_id` | `string` | no |
| `role` | `'admin' \| 'user'` | no |
| `promoted_by` | `string \| null` | yes |
| `created_at` | `string` | no |

Hierarchy rule: an admin may revoke only admins whose `promoted_by = self`; never self.

### 1.5 `favorites`

PK `id`; unique `(user_id, property_id)`; FKs → users, properties.
`{ id, user_id, property_id, created_at }`

### 1.6 `owner_views` (contact reveal audit)

PK `id`; FKs → users, properties. Insert-only (no update/delete).
`{ id, user_id, property_id, viewed_at }`

### 1.7 `property_flags` (reports)

PK `id`; FKs → users, properties; unique `(property_id, user_id)` (one report per user per listing).
`{ id, property_id, user_id, reason: FlagReason, details: string|null, created_at }`
Insert-only; AFTER INSERT trigger recomputes `properties.flag_count` and auto-suspends at ≥10 unless `verified_by_admin`.

### 1.8 `property_comments`

PK `id`; FKs `property_id → properties.id`, `user_id → users.id`, `parent_id → property_comments.id` (self, threaded).
`{ id, property_id, user_id, parent_id: string|null, body: string (1..1000), created_at }`
Read DTO enriches with `author_name: string|null` (from profiles) and `is_owner: boolean` (author == property owner).

### 1.9 `page_views` (analytics)

`{ id, path (≤500), referrer: string|null (≤500), user_id: string|null, session_id: string|null, user_agent: string|null, created_at }`
Privacy: `user_agent` always stored `null`; `session_id` stored as SHA-256(`sessionId:YYYY-MM-DD:rentiz-page-view`) truncated to 32 hex chars (daily-rotating pseudonym). Admin-read only.

### 1.10 `password_reset_codes` (backend-only)

`{ id, email, code_hash (SHA-256 of "email:code:secret"), expires_at (+10 min), consumed_at: string|null, attempts int (max 5), created_at }`
Cleanup trigger purges rows older than 1 hour past expiry / 1 day old.

### 1.11 `otp_rate_limits` (backend-only)

`{ id bigint, identifier (lowercased email), ip: string|null, kind: 'recovery'|'signup', created_at }`
Policy constants (from `check_otp_rate_limit`): 15-min window, 60 s per-email cooldown, 4 sends/email/window, 10 sends/IP/window, rows older than 1 h purged.

### 1.12 Email infrastructure tables (service-role only)

| Table | Shape |
|---|---|
| `email_send_log` | `{ id, message_id: string\|null, template_name, recipient_email, status ('pending'\|'sent'\|'failed'), error_message: string\|null, metadata: json\|null, created_at }` |
| `email_send_state` | `{ id=1, retry_after_until: ts\|null, batch_size=10, send_delay_ms=200, auth_email_ttl_minutes=15, transactional_email_ttl_minutes=60, updated_at }` |
| `email_unsubscribe_tokens` | `{ id, token, email, created_at, used_at: ts\|null }` |
| `suppressed_emails` | `{ id, email, reason, metadata: json\|null, created_at }` |

### 1.13 `blog_posts`

Present in schema, **not referenced by any current UI code**. Shape: `{ id, author_id, title, slug, excerpt|null, content, cover_image_url|null, published boolean, published_at|null, created_at, updated_at }`. Port only if the blog is planned.

### 1.14 Entity relationships

```
users(auth) 1─1 profiles
users 1─* user_roles (promoted_by → users)
users 1─* properties (owner_id, nullable)
properties 1─* favorites *─1 users        (unique user+property)
properties 1─* property_flags *─1 users   (unique user+property)
properties 1─* owner_views *─1 users
properties 1─* property_comments *─1 users (parent_id self-FK)
```

### 1.15 Canonical public Property DTO (exact shape the frontend consumes)

`PropertyRow` — every field below is present; owner PII is absent by design.

```json
{
  "id": "uuid",
  "public_id": "K7QM2XPA",
  "owner_id": "uuid|null",
  "city_id": "bengaluru",
  "title": "Sunlit 2BHK in Koramangala",
  "description": "string|null",
  "bhk_type": "2BHK",
  "rent": 32000,
  "deposit": 100000,
  "is_lease": false,
  "property_type": "Apartment",
  "furnishing": "Semi",
  "parking_2w": true,
  "parking_4w": false,
  "built_up_area_sqft": 1050,
  "property_age": "<5 years",
  "bathrooms": 2,
  "floor": 3,
  "total_floors": 5,
  "availability": "Immediate",
  "available_from": "2026-08-01",
  "preferred_tenants": ["Family", "Couple Friendly"],
  "non_veg_allowed": true,
  "has_gym": false,
  "hosting_visit_next_2_days": true,
  "image_urls": ["https://.../signed.jpg"],
  "area": "Koramangala 4th Block",
  "lat": 12.9352,
  "lng": 77.6245,
  "flag_count": 0,
  "suspended": false,
  "suspended_until": null,
  "verified_by_admin": false,
  "appeal_status": "none",
  "appeal_note": null,
  "archived": false,
  "listed_by": "owner",
  "created_at": "2026-07-29T09:14:15.123Z"
}
```

---

## 2. API Endpoints Specification

All current calls are RPC-style server functions; the table below maps each to a REST endpoint.

### 2.0 Endpoint index

| Current impl | Method & path | Auth | UI surface |
|---|---|---|---|
| `listProperties` | `GET /api/v1/properties` | Public | `/map` (map + sidebar + search) |
| `createProperty` | `POST /api/v1/properties` | Bearer | `ListPropertyModal` |
| `updateProperty` | `PATCH /api/v1/properties/{id}` | Bearer (owner) | `/my-listings` edit |
| `archiveProperty` / `ownerWithdrawListing` | `PATCH /api/v1/properties/{id}/archive` | Bearer (owner) | `/my-listings` |
| `listMyProperties` | `GET /api/v1/me/properties` | Bearer | `/my-listings` |
| `myListingsAccess` | `GET /api/v1/me/properties/access` | Bearer | sidebar nav gating |
| `listMyFavorites` | `GET /api/v1/me/favorites` | Bearer | `/favorites` |
| `listMyFavoriteIds` | `GET /api/v1/me/favorites/ids` | Bearer | heart state on map |
| `toggleFavorite` | `PUT /api/v1/properties/{id}/favorite` | Bearer | `PropertyDetails` |
| `getFavoriteCount` | `GET /api/v1/properties/{id}/favorite-count` | Public | `PropertyDetails` |
| `revealOwnerContact` | `POST /api/v1/properties/{id}/reveal-contact` | Bearer | "Get owner contact" |
| `setMyPhone` | `PUT /api/v1/me/phone` | Bearer | inline phone capture |
| `flagProperty` | `POST /api/v1/properties/{id}/flags` | Bearer | `ReportPropertyDialog` |
| `getMyListingFlagSummary` | `GET /api/v1/me/properties/{id}/flag-summary` | Bearer (owner) | `/my-listings` safety panel |
| `appealListingSuspension` | `POST /api/v1/me/properties/{id}/appeal` | Bearer (owner) | `/my-listings` |
| `listComments` | `GET /api/v1/properties/{id}/comments` | Public | `PropertyComments` |
| `addComment` | `POST /api/v1/properties/{id}/comments` | Bearer | `PropertyComments` |
| `deleteComment` | `DELETE /api/v1/comments/{id}` | Bearer (author/admin) | `PropertyComments` |
| `getMyProfile` | `GET /api/v1/me/profile` | Bearer | `/profile` |
| `updateMyProfile` | `PATCH /api/v1/me/profile` | Bearer | `/profile` |
| `submitFeedback` | `POST /api/v1/feedback` | Bearer | `/feedback` |
| `recordPageView` | `POST /api/v1/analytics/page-views` | Public | `__root` route change |
| `checkAmAdmin` | `GET /api/v1/me/is-admin` | Bearer | admin nav |
| `whoAmI` | `GET /api/v1/me` | Bearer | admin users page |
| `getAdminStats` | `GET /api/v1/admin/stats` | Bearer+Admin | `/admin` |
| `listAllProperties` | `GET /api/v1/admin/properties` | Bearer+Admin | `/admin/listings` |
| `adminUpdateProperty` | `PATCH /api/v1/admin/properties/{id}` | Bearer+Admin | edit modal |
| `setPropertySuspended` | `PATCH /api/v1/admin/properties/{id}/suspension` | Bearer+Admin | `/admin/listings` |
| `deletePropertyAdmin` | `DELETE /api/v1/admin/properties/{id}` | Bearer+Admin | `/admin/listings` |
| `adminBulkListingAction` | `POST /api/v1/admin/properties/bulk` | Bearer+Admin | `/admin/listings` |
| `clearPropertyFlags` | `POST /api/v1/admin/properties/{id}/clear-flags` | Bearer+Admin | `/admin/listings` |
| `listPropertyFlags` | `GET /api/v1/admin/properties/{id}/flags` | Bearer+Admin | flags drawer |
| `listPropertyReveals` | `GET /api/v1/admin/properties/{id}/reveals` | Bearer+Admin | reveals drawer |
| `listAdminAppeals` | `GET /api/v1/admin/appeals` | Bearer+Admin | `/admin/moderation` |
| `resolveAppeal` | `POST /api/v1/admin/appeals/{propertyId}/resolve` | Bearer+Admin | `/admin/moderation` |
| `listAdminUsers` | `GET /api/v1/admin/users` | Bearer+Admin | `/admin/users` |
| `promoteUser` | `POST /api/v1/admin/users/{userId}/roles` | Bearer+Admin | `/admin/users` |
| `supabase.auth.signUp` | `POST /api/v1/auth/signup` | Public | `/auth` step 1 |
| `supabase.auth.signInWithPassword` | `POST /api/v1/auth/login` | Public | `/auth` |
| `lovable.auth.signInWithOAuth('google')` | `GET /api/v1/auth/oauth/google` → callback | Public | `/auth` |
| `supabase.auth.getSession/getUser` | `GET /api/v1/auth/session` | Bearer | `use-auth`, route gate |
| `supabase.auth.signOut` | `POST /api/v1/auth/logout` | Bearer | sidebar, profile |
| `supabase.auth.updateUser({password})` | `POST /api/v1/auth/password` | Bearer | `/reset-password` |
| `POST /api/public/otp-send` | `POST /api/v1/auth/otp/send` | Public | `/auth` forgot-password, resend verify |
| `POST /api/public/otp-verify-reset` | `POST /api/v1/auth/otp/verify-reset` | Public | `/auth` reset step |

---

### 2.1 `GET /api/v1/properties` — public map feed

**Auth:** Public. **Purpose:** all visible listings for a city; drives map pins, clustering, filter sidebar, and search.

Query params:

| Param | Type | Required | Current behaviour |
|---|---|---|---|
| `cityId` | string (1..40) | yes | `WHERE city_id = ?` |

Fixed server-side behaviour (keep identical): `archived = false`, `(suspended = false OR suspended_until < now())`, `ORDER BY created_at DESC`, `LIMIT 500`, projection = public columns only (no owner PII).

> All other filtering (rent range, BHK, furnishing, parking, tenants, gym, non-veg, hosting-visit, flagless-only, property type, text search) is **client-side today**. Optional Spring extension (backwards-compatible): `minRent`, `maxRent`, `bhk[]`, `propertyType[]`, `furnishing[]`, `availability[]`, `parking2w`, `parking4w`, `nonVeg`, `gym`, `hostingVisit`, `tenants[]`, `maxFlags`, `q`, `page`, `size`, `sort`. Keep the unpaginated array shape or version the change — the UI expects a bare JSON array.

**200 Response:** `PropertyRow[]` (see §1.15).

---

### 2.2 `POST /api/v1/properties` — publish a listing

**Auth:** Bearer. **UI:** `ListPropertyModal` (location pinned on the main map).

Request body:

```json
{
  "city_id": "bengaluru",
  "title": "Sunlit 2BHK in Koramangala 4th Block",
  "description": "string|null",
  "bhk_type": "2BHK",
  "rent": 32000,
  "deposit": 100000,
  "is_lease": false,
  "property_type": "Apartment",
  "furnishing": "Semi",
  "parking_2w": true,
  "parking_4w": false,
  "built_up_area_sqft": 1050,
  "property_age": "<5 years",
  "bathrooms": 2,
  "floor": 3,
  "total_floors": 5,
  "availability": "Immediate",
  "available_from": null,
  "preferred_tenants": ["Family"],
  "non_veg_allowed": false,
  "has_gym": false,
  "hosting_visit_next_2_days": false,
  "image_urls": ["https://.../a.jpg"],
  "area": "Koramangala 4th Block",
  "lat": 12.9352,
  "lng": 77.6245,
  "owner_name": "Ramesh K.",
  "owner_whatsapp": "9876543210",
  "owner_email": "owner@example.com",
  "listed_by": "owner",
  "hp": "",
  "ts": 1785000000000
}
```

Validation (Bean Validation equivalents):

| Field | Rule |
|---|---|
| `city_id` | `@NotBlank @Size(max=40)` |
| `title` | trimmed, `@Size(min=6,max=120)` |
| `description` | nullable, ≤2000 |
| `rent` | int 500..2_000_000 |
| `deposit` | int 0..20_000_000 |
| `built_up_area_sqft` | nullable int 50..50_000 |
| `bathrooms` | int 0..20 |
| `floor` | nullable int -2..200 |
| `total_floors` | nullable int 0..200 |
| `preferred_tenants` | ≤4, each in tenant enum |
| `image_urls` | 1..2 valid URLs, each ≤2000 chars |
| `lat` / `lng` | -90..90 / -180..180 |
| `owner_name` | 2..80 |
| `owner_whatsapp` | `^[0-9]{10}$` |
| `owner_email` | valid email ≤160 |
| `listed_by` | `owner` \| `broker` (default `owner`) |
| `hp` | honeypot — must be empty, else 400 `"Bot detected"` |
| `ts` | form-open epoch ms — reject if `now - ts < 1500` with `"Form submitted too quickly. Please try again."` |

Server side effects: `owner_id` = JWT subject; `public_id` generated; sends `listing-confirmation` email to `owner_email` (idempotency key `listing-confirm-{id}`).

**200 Response:** `{ "id": "uuid" }`

---

### 2.3 `PATCH /api/v1/properties/{id}` — owner edit

**Auth:** Bearer, scoped `owner_id = subject` (no rows updated → treat as 403/404).
Body: any subset of the §2.2 create fields (same validation), excluding `hp`/`ts`.
**200:** `{ "ok": true }`

### 2.4 `PATCH /api/v1/properties/{id}/archive`

Body `{ "archived": true }` → **200** `{ "ok": true, "archived": true }`. Owner-scoped. `ownerWithdrawListing` is the same operation with `archived: true` fixed.

### 2.5 Owner listing reads

- `GET /api/v1/me/properties` → `PropertyRow[]`, `owner_id = subject`, `ORDER BY created_at DESC`, `LIMIT 500`. **Includes archived and suspended rows** (owner must see hidden listings).
- `GET /api/v1/me/properties/access` → `{ "hasActive": true, "activeCount": 2, "totalCount": 5 }` where active = `archived=false AND suspended=false`.

### 2.6 `POST /api/v1/properties/{id}/reveal-contact`

**Auth:** Bearer. **UI:** `PropertyDetails` → "Get owner contact".

Preconditions & effects:
1. Caller's `profiles.phone` must match `^[0-9]{10}$`; otherwise respond **400** with body `{ "error": "PHONE_REQUIRED" }` — the UI keys off this exact string to render the inline "add your mobile number" form.
2. Insert an `owner_views` row (audit; also the analytics source).
3. Email owner `contact-revealed` (skipped when viewer is the owner), including total reveal count and seeker name.

**200 Response:**

```json
{ "owner_name": "Ramesh K.|null", "owner_phone": "9876543210", "owner_email": "owner@example.com|null" }
```

`PUT /api/v1/me/phone` — body `{ "phone": "98765 43210" }`; server strips non-digits, requires 10 digits; upserts `profiles.phone`. **200:** `{ "ok": true, "phone": "9876543210" }`.

### 2.7 Favorites

| Endpoint | Auth | Body | Response |
|---|---|---|---|
| `PUT /api/v1/properties/{id}/favorite` | Bearer | — | `{ "favorited": true }` / `{ "favorited": false }` (toggle: delete if exists else insert) |
| `GET /api/v1/me/favorites` | Bearer | — | `PropertyRow[]` (favorited AND `archived=false`, newest first) |
| `GET /api/v1/me/favorites/ids` | Bearer | — | `["uuid", ...]` |
| `GET /api/v1/properties/{id}/favorite-count` | Public | — | `{ "count": 12 }` |

### 2.8 `POST /api/v1/properties/{id}/flags` — report a listing

**Auth:** Bearer. **UI:** `ReportPropertyDialog`.

```json
{ "reason": "fake_listing", "details": "string|null", "hp": "", "ts": 1785000000000 }
```

Rules, in order:
1. `hp` non-empty → 400 `"Bot detected"`.
2. `now - ts < 1500` → 400 `"Please take a moment before submitting."`
3. Caller's flags in last 24 h ≥ 10 → 429 `"Daily report limit reached. Please try again later."`
4. `reason == "other"` and blank `details` → 400 `"Please describe the issue when selecting 'Other'."`
5. Target `verified_by_admin` → 400 `"This listing has been verified by moderators and cannot be reported."`
6. Duplicate `(property_id, user_id)` → 400 `"You've already reported this listing."`
7. `details` ≤500 chars, trimmed, empty → null.
8. Recompute `flag_count`; if ≥ **10** and not verified → `suspended = true`, `suspended_until = now() + 2 days`.
9. Emails: `flag-raised-on-listing` always (unless self-flag); `listing-suspended` when the listing is suspended.

**200 Response:**

```json
{ "count": 4, "suspended": false, "suspended_until": null, "threshold": 10 }
```

### 2.9 Owner safety / appeals

`GET /api/v1/me/properties/{id}/flag-summary` (owner-scoped; 403 `"Not your listing"`):

```json
{
  "total": 4,
  "threshold": 10,
  "suspended": false,
  "suspended_until": null,
  "verified_by_admin": false,
  "appeal_status": "none",
  "appeal_note": null,
  "breakdown": { "fake_listing": 3, "spam_or_scam": 1 }
}
```

`POST /api/v1/me/properties/{id}/appeal` — body `{ "note": "string 10..1000" }`; sets `appeal_status='pending'`, `appeal_note=note`; emails `appeal-received`. **200:** `{ "ok": true }`.

### 2.10 Comments

`GET /api/v1/properties/{id}/comments` — Public; `ORDER BY created_at ASC`, `LIMIT 500`.

```json
[{
  "id": "uuid", "property_id": "uuid", "user_id": "uuid",
  "parent_id": "uuid|null", "body": "string",
  "created_at": "2026-07-29T09:14:15.123Z",
  "author_name": "Asha|null", "is_owner": false
}]
```

`POST /api/v1/properties/{id}/comments` — Bearer; body `{ "body": "1..1000", "parentId": "uuid|null" }`; emails owner `comment-on-listing` (skip self). **200:** `{ "id": "uuid" }`.

`DELETE /api/v1/comments/{id}` — Bearer; allowed for the author or an admin. **200:** `{ "ok": true }`.

### 2.11 Profile

`GET /api/v1/me/profile` → (synthesises an empty profile when the row is missing)

```json
{ "id": "uuid", "full_name": "string|null", "phone": "string|null",
  "phone_verified": false, "email": "user@example.com|null",
  "created_at": "2026-07-29T09:14:15.123Z" }
```

`PATCH /api/v1/me/profile` — body `{ "full_name": "1..80 | null" }` → `{ "ok": true }`.

### 2.12 Feedback

`POST /api/v1/feedback` — Bearer; body `{ "category": "Bug report", "message": "10..4000", "hp": "", "ts": 1785000000000 }`. Same honeypot/timing rules. Emails `feedback-received` to `ADMIN_EMAIL` and `feedback-confirmation` to the user. **200:** `{ "ok": true }`. Not persisted today — consider a `feedback` table in the rebuild.

### 2.13 Analytics

`POST /api/v1/analytics/page-views` — Public; body `{ "path": "≤500", "referrer": "≤500|null", "sessionId": "≤80|null" }`. Server hashes `sessionId` (see §1.9), forces `user_agent = null`. **200:** `{ "ok": true }`.

### 2.14 Admin

`GET /api/v1/admin/stats`:

```json
{
  "totals": { "properties": 0, "active": 0, "suspended": 0, "newLast24h": 0,
              "contactReveals": 0, "contactReveals24h": 0, "flags": 0,
              "favorites": 0, "users": 0, "pageViews": 0, "pageViews24h": 0 },
  "byCity": [{ "name": "bengaluru", "value": 42 }],
  "byBhk":  [{ "name": "2BHK", "value": 18 }],
  "viewsByDay":   [{ "date": "2026-07-23", "count": 5 }],
  "trafficByDay": [{ "date": "2026-07-23", "count": 120, "visitors": 44 }],
  "topPaths": [{ "name": "/map", "value": 300 }]
}
```
Windows: 24 h and 7 d; `viewsByDay`/`trafficByDay` always contain exactly 7 dated buckets (oldest first, zero-filled); `topPaths` top 8; `visitors` = distinct hashed `session_id` per day.

`GET /api/v1/admin/properties?search=&suspended=all|yes|no` — `LIMIT 200`, newest first. `search` matches (case-insensitive, contains) `title`, `area`, `owner_name`, `owner_whatsapp`, `public_id`; sanitise the term (current code strips `, ( ) * " ' \ %` and caps at 80 chars — with JPA/prepared statements just escape `%`/`_`). Response rows (admin-only, PII included):

```json
[{ "id":"uuid","public_id":"K7QM2XPA","title":"…","city_id":"chennai","bhk_type":"2BHK",
   "rent":32000,"area":"…","owner_name":"…","owner_whatsapp":"9876543210","owner_id":"uuid",
   "flag_count":0,"suspended":false,"suspended_until":null,"verified_by_admin":false,
   "appeal_status":"none","appeal_note":null,"created_at":"…" }]
```

`PATCH /api/v1/admin/properties/{id}` — body: optional `title` (3..120), `description|null` (≤2000), `rent`, `deposit`, `area|null`, `bhk_type`, `furnishing`, `listed_by`, `verified_by_admin`, `archived`, `suspended`, `owner_name|null` (≤80), `owner_whatsapp` (10 digits), `owner_email|null`. Setting `suspended=false` also clears `suspended_until`. → `{ "ok": true }`

`PATCH /api/v1/admin/properties/{id}/suspension` — `{ "suspended": true }` → `{ "ok": true }`
`DELETE /api/v1/admin/properties/{id}` → `{ "ok": true }` (hard delete)

`POST /api/v1/admin/properties/bulk` — `{ "ids": ["uuid"×1..200], "action": "suspend|unsuspend|verify|archive|unarchive|delete" }` → `{ "ok": true, "count": 3 }`
- `suspend` → `suspended=true`, `suspended_until = now()+2d`
- `unsuspend` → `suspended=false`, `suspended_until=null`
- `verify` → `verified_by_admin=true`, `suspended=false`, `suspended_until=null`, `flag_count=0`
- `archive`/`unarchive` → `archived` toggle; `delete` → hard delete

`POST /api/v1/admin/properties/{id}/clear-flags` — `{ "note": "≤500|null" }`; atomic (single transaction, was a DB function): delete all flags, `flag_count=0`, `suspended=false`, `suspended_until=null`, `verified_by_admin=true`, `appeal_status: pending→approved`, `appeal_note = note ?? existing`. → `{ "ok": true }`

`GET /api/v1/admin/properties/{id}/flags` → `[{ "id","reason","details","created_at","user_id" }]`, newest first, `LIMIT 100`.

`GET /api/v1/admin/properties/{id}/reveals` → `[{ "user_id","viewed_at","full_name":"…|null","phone":"…|null" }]`, newest first, `LIMIT 200`.

`GET /api/v1/admin/appeals` → pending appeals (`appeal_status='pending'`), `ORDER BY updated_at DESC`, `LIMIT 200`, fields `id, public_id, title, area, city_id, flag_count, suspended, suspended_until, appeal_status, appeal_note, owner_id, updated_at`.

`POST /api/v1/admin/appeals/{propertyId}/resolve` — `{ "outcome": "approved|rejected", "note": "≤500|null" }`. `approved` runs the clear-flags routine; `rejected` sets `appeal_status='rejected'`, `appeal_note=note`. Emails `appeal-resolved`. → `{ "ok": true }`

`GET /api/v1/admin/users` → newest 200 profiles enriched with roles:

```json
[{ "id":"uuid","full_name":"…|null","phone":"…|null","created_at":"…",
   "roles":["admin"],"adminPromotedBy":"uuid|null" }]
```

`POST /api/v1/admin/users/{userId}/roles` — `{ "role": "admin", "grant": true|false }`
- grant: upsert `(user_id, 'admin', promoted_by = caller)`, ignore duplicates; self → 400 `"You already have admin."`
- revoke: self → 400 `"You cannot revoke your own admin access."`; not an admin → 400 `"That user is not an admin."`; `promoted_by != caller` → 403 `"You can only revoke admins that you promoted."`
→ `{ "ok": true }`

`GET /api/v1/me/is-admin` → `{ "isAdmin": true }`. Bootstrap rule: if the JWT email equals env `ADMIN_EMAIL`, self-grant the `admin` role and return true.
`GET /api/v1/me` → `{ "userId": "uuid" }`.

### 2.15 Auth endpoints

| Endpoint | Auth | Body / params | Response |
|---|---|---|---|
| `POST /api/v1/auth/signup` | Public | `{ email, password, full_name, phone? }` + email-redirect target | `{ "userId": "uuid", "emailVerificationRequired": true }` |
| `POST /api/v1/auth/login` | Public | `{ email, password }` | `{ access_token, refresh_token, expires_at, user }` |
| `POST /api/v1/auth/refresh` | Public | `{ refresh_token }` | same as login |
| `GET /api/v1/auth/session` | Bearer | — | `{ "user": { "id","email","user_metadata":{...} } }` or 401 |
| `POST /api/v1/auth/logout` | Bearer | — | `204` |
| `POST /api/v1/auth/password` | Bearer | `{ "password": "…" }` | `{ "ok": true }` |
| `GET /api/v1/auth/oauth/google` | Public | `?redirect_uri=<same-origin public URL>` | 302 to Google; callback issues tokens |
| `POST /api/v1/auth/verify-email` | Public | `{ email, token }` | `{ "ok": true }` |

Client contracts to preserve: signup requires email confirmation before login (UI shows an explicit "check your inbox" step); `full_name` is read back from user metadata into `profiles`; `phone` is optional at signup but mandatory before revealing owner contact (§2.6); Google OAuth must return to a **public** callback route, then the app navigates on.

Password policy (enforced on reset today, mirror it on signup): 6..72 chars, ≥1 lowercase, ≥1 uppercase, ≥1 digit, ≥1 symbol; confirm-password matching is a client rule.

`POST /api/v1/auth/otp/send` — Public.

```json
{ "email": "user@example.com", "kind": "recovery" }
```

Rate limiting (per email + per IP; IP from `X-Forwarded-For` → `CF-Connecting-IP` → `X-Real-IP`): 60 s cooldown, 4/email/15 min, 10/IP/15 min. Recovery path: generate a cryptographically random 6-digit code, invalidate prior unconsumed codes for that email, store `SHA-256(email:code:secret)` with a 10-minute expiry, render the `recovery` template, log to `email_send_log` (`pending → sent|failed`), attach/reuse an unsubscribe token, then send. Signup path: resend the verification email.

Success **200** (receipt-based — only after the provider accepts):

```json
{ "ok": true, "status": "success", "messageId": "uuid",
  "timestamp": "2026-07-29T09:14:15.123Z", "kind": "recovery",
  "recipient": "user@example.com", "remaining": 3, "cooldown": 60 }
```

Failures the UI already handles: `400 {ok:false,error:"invalid_json"|"invalid_input"}`, `429 {ok:false,status:"rate_limited",error:"cooldown"|"email_quota"|"ip_quota",retryAfter:<s>}` plus a `Retry-After` header, `502 {ok:false,status:"error",error:"send_failed",providerMessage}`, `500 … "server_misconfigured"|"rate_limit_check_failed"`.

`POST /api/v1/auth/otp/verify-reset` — Public.

```json
{ "email": "user@example.com", "code": "123456", "password": "NewPass1!" }
```
`code` must match `^\d{6}$`. Logic: latest unconsumed code for the email → expired/missing → `400 expired_or_invalid`; `attempts >= 5` → consume + `429 too_many_attempts`; hash mismatch → increment attempts + `400 expired_or_invalid`; unknown account → `400 account_not_found`; password update failure → `502 update_failed`. On success set the password, consume the code, return `{ "ok": true, "status": "success" }`.

---

## 3. File Storage & Media

| Item | Current value |
|---|---|
| Bucket | `property-photos` (private) |
| Object key | `{userId}/{uuid}.{ext}` — ext lowercased from the original filename, default `jpg` |
| Upload call site | `ListPropertyModal.onPick` — direct browser → storage upload, `upsert: false`, `contentType` from the `File` |
| Accepted types | `image/*` only (client filter `file.type.startsWith("image/")`) |
| Max size | 6 MB per file |
| Max count | 2 photos per property (`MAX_PROPERTY_IMAGES`) |
| URL form | Signed URL, TTL `60*60*24*365*10` s (10 years), stored verbatim in `properties.image_urls` |
| Overwrite policy | Only the original uploader (path prefix `{userId}/`) may overwrite |
| Consumed in UI | `ListPropertyModal` previews, `PropertyDetails` gallery, `RentizMap` hover cards, `/my-listings`, `/favorites`, `/admin/listings` |

**Spring Boot rebuild recommendation**

1. `POST /api/v1/uploads/property-photos` (Bearer, `multipart/form-data`, field `file`) — validate MIME `image/jpeg|png|webp` and ≤6 MB, store under `{userId}/{uuid}.{ext}` in S3/MinIO, return:
   ```json
   { "key": "userId/uuid.jpg", "url": "https://…signed…", "expiresAt": "2036-07-29T00:00:00Z" }
   ```
2. Enforce ≤2 images server-side on create/update (today it is client-only plus the create-time array bound).
3. Prefer storing the **object key** and minting short-lived signed URLs on read; 10-year signed URLs are effectively permanent links. If you keep full URLs in `image_urls`, keep the field a `text[]`/`jsonb` array of strings so the DTO shape stays identical.

---

## 4. External Integrations

| Integration | Where called | Key / credential | Notes |
|---|---|---|---|
| **Google Maps JS API** (`maps/api/js`, `libraries=marker`) | `src/lib/useGoogleMaps.ts` (loader), `src/components/RentizMap.tsx` (`Map`, `AdvancedMarker`, `LatLngBounds`, geolocation recentre, satellite/hybrid toggle, custom clustering) | browser key `VITE_LOVABLE_CONNECTOR_GOOGLE_MAPS_BROWSER_KEY`, channel `VITE_LOVABLE_CONNECTOR_GOOGLE_MAPS_TRACKING_ID` | Browser-only; HTTP-referrer-restricted key. No server-side Maps calls today. |
| **Google Places Autocomplete (new API)** | `src/components/MapSearchBar.tsx` — `AutocompleteSessionToken`, `AutocompleteSuggestion.fetchAutocompleteSuggestions`, place fetch for lat/lng | same browser key | Locality search on the map. Optionally proxy through Spring (`GET /api/v1/places/autocomplete?q=`, `GET /api/v1/places/{placeId}`) with a server key to hide quota. |
| **Browser Geolocation API** | `RentizMap.tsx` recentre button | — | Client-side; auto-switches the active city to the nearest one. |
| **Google OAuth** | `src/routes/auth.tsx` via `lovable.auth.signInWithOAuth("google", { redirect_uri: origin })` | OAuth client id/secret | Replace with Spring Security OAuth2 client; `redirect_uri` must be a public same-origin callback. |
| **Transactional email provider** | `src/lib/email/dispatch.ts`, `src/routes/api/public/otp-send.ts`, `src/routes/lovable/email/**` | `LOVABLE_API_KEY`, `LOVABLE_SEND_URL` | From `Rentiz <noreply@notify.rentiz.in>`, sender domain `notify.rentiz.in`. Queue-backed with retry/DLQ, per-message `messageId`, idempotency keys, unsubscribe tokens, suppression list, `email_send_log` audit. Map to any SMTP/API provider (SES/Postmark) + an outbox table. |
| **Payments** | — | — | **None integrated.** No Stripe/Paddle/Razorpay code exists. |

### 4.1 Email templates to port

| Template | Trigger | Data fields |
|---|---|---|
| `signup` | account signup | confirmation link/code |
| `magic-link` | passwordless link | link |
| `recovery` | `otp/send` kind=`recovery` | `siteName`, `token` (6-digit) |
| `email-change`, `reauthentication`, `invite` | auth flows | provider fields |
| `listing-confirmation` | property created | `ownerName`, `propertyTitle`, `propertyArea`, `publicId`, `rent`, `listingUrl` |
| `contact-revealed` | reveal-contact | `ownerName`, `propertyTitle`, `propertyArea`, `publicId`, `revealCount`, `seekerName` |
| `comment-on-listing` | comment added | `ownerName`, `propertyTitle`, `publicId`, `commenterName`, `commentBody`, `listingUrl` |
| `flag-raised-on-listing` | flag inserted | `ownerName`, `propertyTitle`, `publicId`, `reason`, `totalFlags`, `threshold`, `listingUrl` |
| `listing-suspended` | auto-suspend at threshold | `ownerName`, `propertyTitle`, `publicId`, `flagCount`, `suspendedUntil`, `appealUrl` |
| `appeal-received` | owner files appeal | `ownerName`, `propertyTitle`, `publicId`, `note` |
| `appeal-resolved` | admin resolves | `ownerName`, `propertyTitle`, `publicId`, `outcome`, `note`, `listingUrl` |
| `feedback-received` | feedback submitted (→ admin) | `fromName`, `fromEmail`, `category`, `message` |
| `feedback-confirmation` | feedback submitted (→ user) | `userName`, `message` |

---

## 5. Cross-cutting rules for the Spring Boot rebuild

1. **Authorization layers.** Row rules currently enforced by RLS must move into the service layer: public reads filter `archived=false AND (suspended=false OR suspended_until<now())`; owner writes filter `owner_id = principal`; admin endpoints check the `user_roles` table (never a claim or client flag).
2. **Column-level PII.** Two projections of `properties`: `PropertyPublicDto` (§1.15) and `PropertyAdminDto` (adds owner fields). Owner PII leaves the system only through §2.6 and admin endpoints.
3. **Bot prevention.** Honeypot field `hp` (must be empty) plus `ts` form-open timestamp (≥1500 ms dwell) on create-property, flag, and feedback.
4. **Idempotent email sends.** Every send carries an idempotency key (e.g. `reveal-{propertyId}-{userId}`, `listing-confirm-{id}`); keep an outbox/log table so retries don't duplicate mail.
5. **Automated moderation.** Flag threshold `10`, cooldown `2 days`; `verified_by_admin=true` makes a listing unflaggable and clears counts. Implement as a transactional service method (replacing the DB trigger) or keep it as a trigger.
6. **Derived / trigger behaviour to reimplement.** `public_id` generation with uniqueness retry; `updated_at` touch on update; profile row creation on user signup; `flag_count` recomputation; expiry cleanup for `password_reset_codes` and `otp_rate_limits`.
7. **Suggested indexes** (mirroring current ones): `properties(city_id, archived, suspended, created_at DESC)`, `properties(owner_id, created_at DESC)`, unique `properties(public_id)`, `favorites(user_id, property_id)` unique, `property_flags(property_id, user_id)` unique, `property_flags(user_id, created_at)`, `owner_views(property_id, viewed_at DESC)`, `property_comments(property_id, created_at)`, `page_views(created_at)`, `password_reset_codes(email, created_at DESC)`, `otp_rate_limits(identifier, created_at)`, `otp_rate_limits(ip, created_at)`.
8. **Response shape fidelity.** List endpoints currently return bare JSON arrays and mutations return small flat objects (`{ok:true}`, `{id}`, `{favorited}`, `{count,suspended,suspended_until,threshold}`). Changing these to a wrapped envelope requires frontend changes — version the API if you want envelopes.
