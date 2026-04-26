# Volley Reservations

A Spring Boot backend for a volleyball field reservation application. Users can register, log in, browse field availability, add time slots to a shopping cart, and confirm bookings.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.4 |
| ORM | Spring Data JPA / Hibernate |
| Database | MySQL |
| Security | Spring Security (form login, BCrypt) |
| Templating | Thymeleaf + Thymeleaf Security Extras |
| Frontend | Bootstrap 5.3.2, Vanilla JS |
| Testing | JUnit 5, Spring Security Test, Selenium 4 |
| Build | Maven |

---

## Project Structure

```
src/main/java/com/example/volley_reservations/
├── VolleyReservationsApplication.java   # Entry point
├── config/
│   └── SecurityConfig.java              # Security rules, BCrypt bean
├── controller/
│   ├── FieldController.java             # Field view + availability matrix
│   ├── LoginController.java             # Login and home pages
│   ├── RegistrationController.java      # User registration
│   └── TemporaryCartController.java     # Shopping cart actions
├── dto/
│   ├── RegistrationRequest.java         # Registration form DTO
│   └── ReservationRequest.java          # Reservation form DTO
├── model/
│   ├── Reservation.java                 # Persistent reservation entity
│   ├── TemporaryReservation.java        # In-memory cart item (not persisted)
│   └── User.java                        # User entity
├── repository/
│   ├── ReservationRepository.java       # JPA queries for reservations
│   └── UserRepository.java              # JPA queries for users
├── security/
│   ├── CustomUserDetails.java           # UserDetails implementation
│   └── CustomUserDetailsService.java    # Loads user from DB for Spring Security
└── service/
    ├── ReservationService.java          # Reservation business logic
    ├── TemporaryReservationService.java # In-memory cart + slot locking
    └── UserService.java                 # User registration + password encoding
```

---

## API Endpoints

### Authentication

| Method | Path | Description |
|---|---|---|
| GET | `/login` | Login form page |
| POST | `/login` | Spring Security handles login |
| GET/POST | `/logout` | Invalidates session, redirects to `/login?logout` |
| GET | `/register` | Registration form page |
| POST | `/register` | Creates a new user account |
| GET | `/home` | Home page (requires auth) |

### Field Reservations

| Method | Path | Description |
|---|---|---|
| GET | `/field/{fieldNumber}` | View field schedule (7-day matrix) |
| POST | `/field/{fieldNumber}` | Create a direct reservation |
| GET | `/field/{fieldNumber}/matrix` | JSON availability matrix (polled by frontend every 30s) |
| GET | `/field/{fieldNumber}/cart` | Redirect to cart view |

### Shopping Cart

| Method | Path | Description |
|---|---|---|
| POST | `/cart/add` | Add a time slot to the cart |
| GET | `/cart/view` | View cart contents and total price |
| POST | `/cart/delete` | Remove a cart item |
| POST | `/cart/confirm` | Persist all cart items as confirmed reservations |
| POST | `/cart/back` | Navigate back to the field page |

All endpoints except `/login`, `/register`, and static assets require authentication.

---

## Data Models

### User
| Column | Type | Notes |
|---|---|---|
| `user_id` | BIGINT (PK) | Auto-increment |
| `username` | VARCHAR | Unique, not null |
| `password` | VARCHAR | BCrypt-encoded |
| `is_active` | BOOLEAN | Default true |

### Reservation
| Column | Type | Notes |
|---|---|---|
| `reservation_id` | BIGINT (PK) | Auto-generated |
| `reservation_date` | DATE | |
| `reservation_time` | VARCHAR | E.g. `"08:00 - 09:30"` |
| `field_number` | INT | 1 or 2 |
| `user_id` | BIGINT (FK) | References `users.user_id` |

### TemporaryReservation (in-memory only)
Held in a `ConcurrentHashMap` per user session. Each item has a 10-minute TTL and locks the slot to prevent double-booking by other users.

---

## Key Features

- **Availability matrix** — 7 days × 9 time slots (08:00–20:00, 90-minute intervals). Slots are shown as free, locked (in someone's cart), or booked.
- **Shopping cart** — Users add slots to a temporary cart before confirming. Unconfirmed slots expire after 10 minutes.
- **Slot locking** — A slot in any user's cart is locked for all other users in real time.
- **Auto-cleanup** — Past reservations are deleted automatically when a field page is loaded.
- **Pricing** — Fixed rate of 20 lei per slot; the cart displays the running total.

---

## Setup & Running

### Prerequisites
- Java 21
- Maven
- MySQL running locally

### Database
Create a MySQL database named `volei`:
```sql
CREATE DATABASE volei;
```

### Configuration
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/volei
spring.datasource.username=root
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
```
Hibernate will create or update the schema automatically on first run.

### Run
```bash
./mvnw spring-boot:run
```
The application starts on `http://localhost:8080`.

---

## Testing

**Unit tests** cover `UserService`, `ReservationService`, and `TemporaryReservationService`.

**Selenium E2E tests** cover login, registration, home page, cart flow, reserved-spot behavior, and price calculation.

```bash
./mvnw test
```
