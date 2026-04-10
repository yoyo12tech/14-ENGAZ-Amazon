
---

# 📦 Amazon Microservices Project — Complete Extraction (Grader-Proof)

---

# I. Universal Project Architecture & Rules

## 1. Project Structure & Configuration

* **Multi-Module Architecture:**
    * Maven multi-module project.
    * Root project is an **aggregator only** (`<packaging>pom</packaging>`).
    * The root is **NOT a parent POM**.
    * Each service maintains its own Spring Boot parent.

* **Dependencies & Environment (Strict):**
    * **JDK Version:** 25.
    * **Required Dependencies:** Spring Web, Spring Boot DevTools, Spring Data JPA, PostgreSQL Driver, Rest Repositories.

* **Mandatory Package Structure (per service):**
    * `model/`
    * `repository/`
    * `service/`
    * `controller/`
    * `dto/`

* **Separation of Concerns (Strict Rule):**
    * **Controller:** Handles HTTP requests/responses only.
    * **Service:** Contains all business logic and computations.
    * **Repository:** Handles database access only.
    * **DTO Layer:** Used for shaping responses and requests (especially for non-trivial outputs).

---

## 2. The "Zero-Feature" Baseline (Crucial for Grading)

* **Mandatory CRUD:** Before starting any numbered features (S1-F1, etc.), you **MUST** implement basic CRUD (Create, Read, Update, Delete) operations for **every single entity** in your service. The grader will test these.
* **Mandatory Health Endpoints:** Every service must have a simple GET endpoint returning a `200 OK` with the string `"OK"`.
    * User Service: `GET /api/users/health`
    * Product Service: `GET /api/products/health`
    * Order Service: `GET /api/orders/health`
    * Shipping Service: `GET /api/shipments/health`
    * Billing Service: `GET /api/transactions/health`

---

## 3. Database & Data Access Rules

* **Single Shared Database:**
    * PostgreSQL database: `amazondb`
    * Managed via `docker-compose`.

* **Service Isolation Constraint:**
    * Each service defines JPA entities **ONLY for its own tables**.

* **Cross-Service Data Access (Critical Rule):**
    * MUST use **native SQL queries (`@Query`)**.
    * MUST NOT use JPA relationships across services.
    * Cross-table joins (e.g., users ↔ orders) are done via SQL.

---

## 4. JSONB & Enum Handling (PostgreSQL-Specific)

* **JSONB Dependency:** Must add `jackson-databind` to every service.
* **Mapping:** Map JSON fields as `Map<String, Object>`.
* **Enums:** Must be stored as **Strings in DB** (requires `@Enumerated(EnumType.STRING)` configuration).

---

## 5. Global Error Handling Standard

Consistent HTTP responses:
* `200 OK` → Successful retrieval/update
* `201 Created` → Successful creation
* `400 Bad Request` → Invalid input / logical violation / ownership mismatch
* `404 Not Found` → Entity does not exist
* `403 Forbidden` → Admin action attempted by non-admin.

---

## 6. Git & Team Workflow Naming Quirks (Critical)

* **Package Naming:** MUST NOT contain hyphens.
    * *Correct:* `com.teamXX.amazon.user`, `com.teamXX.amazon.shipping`.
    * *Incorrect:* `com.teamXX.amazon.user-service`.
* **Branch Naming:** Uses the short service name.
    * Format: `feat/<short-service-name>/<feature-ID>/<studentId>`.
    * *Example:* `feat/user/S1-F1/55-8078`, `feat/shipping/S4-F2/55-8078`.
* **Commit Messaging:** Uses the full service name.
    * Format: `feat (<full-service-name>): <description> (<studentId>)`.
    * *Example:* `feat (user-service): added DTO (55-8078)`.

---

# II. User Service (Port 8081)

## 1. Entities & Expected JSONB Structure

### User (`users`)
* **Fields:** `id`, `name`, `email` (unique), `password`, `phone` (unique), `role` (CUSTOMER/ADMIN), `status` (ACTIVE/DEACTIVATED), `preferences` (JSONB), `createdAt`.
* **JSONB `preferences` Expected Keys:** `language`, `notifications` (nested object with `email` and `sms` booleans), `defaultPaymentMethod`, `preferredShipping`.

### ShippingAddress (`shipping_addresses`)
* **Fields:** `id`, `label`, `streetAddress`, `city`, `country`, `zipCode`, `isDefault` (default false), `metadata` (JSONB), `createdAt`.
* **Relationship:** ManyToOne → User.
* **JSONB `metadata` Expected Keys:** `deliveryInstructions`, `gateCode`, `floor`, `apartment`, `landmark`.

---

## 2. Features & Strict Requirements

* **S1-F1 Search Users:** Filters: name, email, role. Case-insensitive partial matching. Return empty list if no results.
* **S1-F2 Update Preferences:** Merge JSONB. Overwrite existing keys, add new keys.
* **S1-F3 Order Summary:**
    * *Mandatory DTO Fields:* `userId`, `name`, `totalOrders`, `completedOrders`, `cancelledOrders`, `totalSpent`, `averageOrderValue`.
    * *Note:* Aggregation requires native SQL join with `orders` table.
* **S1-F4 Deactivate Account (Transactional):** Reject (400) if active orders exist. Otherwise set status = DEACTIVATED.
* **S1-F5 Filter by Preference:** JSONB key/value filtering.
* **S1-F6 Top Buyers:**
    * *Mandatory DTO Fields:* `userId`, `name`, `totalSpent`, `orderCount`.
    * *Validation:* Throw 400 if startDate > endDate.
* **S1-F7 Set Default Address (Transactional):**
    * *Validation:* Throw 404 if user or address not found. **Throw 400 if the address ID belongs to a different user**.
    * *Action:* Unset all other addresses to false, set target to true.
* **S1-F8 Get Profile:**
    * *Mandatory DTO Fields:* `userId`, `name`, `email`, `phone`, `preferences`, `shippingAddresses` (List of objects containing: `label`, `streetAddress`, `city`, `country`, `zipCode`, `isDefault`, `metadata`), `totalAddresses`.
* **S1-F9 Language + Min Orders Filter:** JSONB + SQL join combined.

---

# III. Shipping Service (Port 8084)

## 1. Entity & Expected JSONB Structure

### Shipment (`shipments`)
* **Fields:** `id`, `orderId` (FK via SQL), `carrier`, `trackingNumber` (unique), `status` (PROCESSING, SHIPPED, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, RETURNED), `latitude`, `longitude`, `estimatedDelivery`, `actualDelivery`, `metadata` (JSONB), `lastUpdate`, `createdAt`.
* **JSONB `metadata` Expected Keys:** `weight` (kg), `dimensions` (LxWxH), `method` (GROUND, AIR, EXPRESS), `currentCity`, `deliveryAttempts`, `signatureRequired` (boolean).

---

## 2. Features & Strict Requirements

* **S4-F1 Latest Shipment:** Return most recent shipment for an order by `lastUpdate`.
* **S4-F2 Create Shipment:** Set status = PROCESSING, lastUpdate = now. Return 201 Created.
* **S4-F3 Nearby Shipments:**
    * *Mandatory DTO Fields:* `shipmentId`, `orderId`, `carrier`, `trackingNumber`, `latitude`, `longitude`, `distanceKm`.
    * *Math:* Filter OUT_FOR_DELIVERY. Distance formula: `sqrt((x2-x1)^2 + (y2-y1)^2) * 111`. Sort ascending.
* **S4-F4 Batch Status Update (Transactional):**
    * *Validation (Strict):* **Latitude MUST be between -90 and 90. Longitude MUST be between -180 and 180. Throw 400 if invalid**.
    * *Action:* Update status, coordinates, and `lastUpdate`.
* **S4-F5 Filter by Metadata:** JSONB numeric casting required for `gt` / `lt` operators. Throw 400 for invalid operators.
* **S4-F6 History by Date:** Filter by `lastUpdate`. Sort ascending.
* **S4-F7 Purge Old Data (Transactional):** Calculate cutoff (`today - n days`) and delete via native `@Modifying` query.
* **S4-F8 Carrier Summary:**
    * *Mandatory DTO Fields:* `carrier`, `totalShipments`, `deliveredCount`, `averageDeliveryDays`, `onTimeRate`.
    * *Math Details:* `averageDeliveryDays` = `actualDelivery - createdAt` (cast to days). `onTimeRate` = percentage where `actualDelivery <= estimatedDelivery`.
* **S4-F9 Delayed Shipments:**
    * *Mandatory DTO Fields:* `shipmentId`, `orderId`, `carrier`, `trackingNumber`, `estimatedDelivery`, `daysOverdue`, `deliveryAttempts`.
    * *Logic:* Not delivered/returned AND estimatedDelivery is in the past. `deliveryAttempts` cast from JSONB. `daysOverdue` = `CURRENT_DATE - estimatedDelivery`.