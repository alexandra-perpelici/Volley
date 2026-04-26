# Project Backlog & Status

This file tracks the current state of the Volley Reservations project. Items marked `[x]` are done. Items marked `[ ]` need work. Each item includes enough context for an agent to understand the problem without re-exploring the full codebase.

---

## Core Features

- [x] User registration with username + password (BCrypt encoded), validated via `RegistrationRequest` DTO
- [x] Form-based login and logout via Spring Security
- [x] Field availability matrix — 7 days × 9 time slots (08:00–20:00, 90-minute slots), displayed via Thymeleaf templates in `FieldController`
- [x] AJAX polling every 30 seconds on `/field/{fieldNumber}/matrix` to refresh availability without full page reload
- [x] Shopping cart — users add slots to an in-memory cart (`TemporaryReservationService`) before confirming
- [x] Slot locking — a slot in any user's cart is locked and shown as unavailable to others, using a dual `ConcurrentHashMap` structure in `TemporaryReservationService`
- [x] 10-minute cart expiry — `TemporaryReservation` items have an `expireAt` field; expired items are cleaned up on next cart access
- [x] Confirm cart — `POST /cart/confirm` persists all temporary reservations as `Reservation` entities in the DB
- [x] Past reservation cleanup — `ReservationRepository.deleteAllBeforeToday()` is called on field page load, so stale bookings don't block slots
- [x] Pricing display — fixed 20 lei per slot, total shown in cart view
- [x] Unit tests for `UserService`, `ReservationService`, and `TemporaryReservationService`
- [x] Selenium E2E tests for login, registration, home page, cart flow, reserved-spot display, and price calculation

---

## Dead Code / Cleanup

- [ ] **Delete or implement `DataInitializer.java`** — the entire class body is commented out. It was intended to seed test users and reservations on startup. Either implement it as a proper dev-profile bean (`@Profile("dev")`) or delete the file entirely.
- [ ] **Delete or implement `CleanupConfig.java`** — also fully commented out. It was intended to remove seeded test users on startup. Same decision: implement or delete.
- [ ] **Remove or wire up `CustomUserDetails.java`** — this class implements `UserDetails` but is never used. `CustomUserDetailsService.loadUserByUsername()` returns a `org.springframework.security.core.userdetails.User` instance directly instead of a `CustomUserDetails` instance. Either delete `CustomUserDetails.java` or refactor the service to actually return it (which would be the correct pattern if user-specific data needs to be accessed from the security context later).

---

## Security

- [ ] **Re-enable CSRF protection** — `SecurityConfig` calls `.csrf(csrf -> csrf.disable())` with a comment saying "for simplicity." CSRF must be enabled before any production or public deployment. For Thymeleaf forms this requires adding `th:action` (which Thymeleaf already injects the CSRF token into automatically), so the fix is low-effort.
- [ ] **Move database credentials out of `application.properties`** — `spring.datasource.username` and `spring.datasource.password` are hardcoded as `root` / `s3cur1z4tMYSQL`. These should be read from environment variables (e.g., `${DB_USERNAME}` / `${DB_PASSWORD}`) so the file is safe to commit.
- [ ] **Disable SQL logging in non-dev environments** — `spring.jpa.show-sql=true` prints all SQL to stdout including any data in WHERE clauses. This should be `false` by default and only enabled via a dev profile.
- [ ] **Validate field number bounds in controllers** — `FieldController` and `TemporaryCartController` accept `fieldNumber` as a path variable or request parameter but do not validate that it is 1 or 2. An out-of-range value would not cause a crash (no slot would be found) but could produce confusing empty views or silent failures. Add a `@Min(1) @Max(2)` constraint or an explicit check.

---

## Error Handling

- [ ] **Add a global exception handler** — there is no `@ControllerAdvice` class. Unhandled exceptions (e.g., a user trying to book an already-taken slot, or a DB connection failure) fall through to Spring's default whitelabel error page. A `GlobalExceptionHandler` should catch at minimum `DataIntegrityViolationException` (duplicate reservation) and return a user-friendly error view or redirect with a flash message.
- [ ] **Add custom error pages** — no `src/main/resources/templates/error/` directory exists. Spring Boot will serve its default error page for 404, 403, and 500 errors. Add at least `404.html` and `500.html` Thymeleaf templates.
- [ ] **Handle duplicate username on registration** — `UserService.registerUser()` calls `userRepository.save()` without catching the `DataIntegrityViolationException` that will be thrown if the username already exists (the `username` column has a unique constraint). The registration controller should catch this and return the form with an error message instead of crashing.

---

## Architecture & Configuration

- [ ] **Add a dev/prod profile split** — the project has a single `application.properties` with dev-specific settings (`show-sql=true`, hardcoded local DB URL). Create `application-dev.properties` and `application-prod.properties` so the two environments can be configured independently. The main `application.properties` should only hold non-environment-specific defaults.
- [ ] **Add database migrations (Flyway or Liquibase)** — the project relies on `spring.jpa.hibernate.ddl-auto=update` which mutates the schema automatically. This is risky: it can silently drop columns or fail on production databases. Switching to Flyway would give explicit, versioned, reviewable SQL migrations (`V1__init.sql`, etc.) and make deployments deterministic. The entities are simple enough that the initial migration would be short.
- [ ] **Persist the shopping cart** — `TemporaryReservationService` stores cart state in a `ConcurrentHashMap` on the heap. A server restart wipes all carts in progress. For a more robust solution, temporary reservations should either be stored in the DB with a status column (`PENDING` / `CONFIRMED`) and a scheduled cleanup job, or in a Redis cache. The DB approach requires no new infrastructure and is the simpler fix.

---

## Missing Features

- [ ] **User profile / reservation history page** — there is no route where a logged-in user can see their own confirmed reservations or cancel an upcoming one. The `User` entity has a `reservations` OneToMany collection and the data is available; it just needs a controller route (e.g., `GET /profile`) and a Thymeleaf template.
- [ ] **Reservation cancellation** — users cannot cancel a confirmed booking. A `DELETE /reservation/{id}` (or `POST /reservation/{id}/cancel`) endpoint backed by `ReservationService.cancel()` is needed. Should verify ownership before deleting (the reservation's `user` must match the authenticated user).
- [ ] **Admin role and management interface** — all authenticated users are assigned `ROLE_USER`. There is no admin view to see all reservations across all users or manage fields. `SecurityConfig` already uses role-based access control, so adding `ROLE_ADMIN` and protecting `/admin/**` routes is straightforward once the admin UI is designed.
- [ ] **Configurable number of fields** — the application currently assumes exactly 2 fields (field 1 and field 2) and this is referenced by convention in the controllers and templates. If the number of fields changes, multiple files need editing. Consider making the field list dynamic (driven by a `Field` entity or a config property).
- [ ] **Configurable time slots** — the 9 time slots (08:00–20:00, 90 min each) are hardcoded in the service layer. These should be externalized to configuration so they can be changed without a code change.

---

## Testing

- [ ] **Integration tests** — current tests are either pure unit tests (mocked dependencies) or full Selenium E2E tests. There is nothing in between. Spring Boot's `@SpringBootTest` with an in-memory H2 database (or Testcontainers with MySQL) would allow testing the full HTTP → service → repository → DB stack without a browser, catching issues that unit tests miss.
- [ ] **Test coverage for edge cases in `TemporaryReservationService`** — the concurrency logic (dual-map locking, expiry cleanup) is the most complex part of the codebase and the most likely source of bugs. Tests should explicitly cover: adding an expired item, two users racing to lock the same slot, confirming a cart where one slot has since been booked by another user.
- [ ] **Selenium test stability** — E2E tests that depend on hardcoded credentials, timing, or specific DB state are fragile. Ensure tests create their own data (or use a dedicated test DB) and clean up after themselves so they can run in any order and on a fresh environment.
