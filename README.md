# 🛡️ RateShield — Distributed-Ready API Rate Limiter & Gateway Middleware

RateShield is an enterprise-grade, high-performance API Rate Limiter and Gateway Middleware built using **Java 17**, **Maven**, and **Spring Boot 3.2.0**. 

It acts as an API gateway filter that intercepts incoming HTTP requests in real-time, enforcing rate limits per client (IP / API key) to protect microservices against request flooding, DDoS attacks, brute-force spam, and resource exhaustion.

---

## 📸 Interactive Web Dashboard

RateShield features a built-in single-page web dashboard served directly by Spring Boot at `http://localhost:8080`.

- **Live Analytics Cards:** Real-time count of active rules, 24-hour violation logs, and gateway health.
- **Interactive Burst Simulator:** Test rate limit limits in real-time. Fires rapid request bursts to demonstrate HTTP 429 Too Many Requests responses visually.
- **Dynamic Rule CRUD:** Add, update, toggle, or delete rate limiting paths and algorithms without restarting the application.
- **Top Violators Table:** Tracks top violating client IP addresses asynchronously via event listeners.

---

## 🚀 Resume Bullet Points

> **Copy and paste these bullet points into your resume under the Projects section:**

- **Engineered RateShield, a production-grade API Rate Limiter & Gateway in Java 17 and Spring Boot**, protecting backend services against traffic spikes by processing high-throughput HTTP traffic and enforcing granular client rate limits.
- **Implemented 3 core rate limiting algorithms** (*Token Bucket, Sliding Window Log, Fixed Window Counter*) using the **Strategy & Factory design patterns**, enabling zero-downtime algorithm swapping via dynamic database configs.
- **Designed for high concurrency & thread safety**, leveraging `ConcurrentHashMap`, fine-grained object synchronization, and atomic operations to prevent race conditions during high-volume concurrent request bursts.
- **Architected using Spring Servlet Filter & Observer Pattern** (`ApplicationEventPublisher`), intercepting incoming requests at low latency (`@Order(1)`) and asynchronously auditing rate limit violations to an H2/JPA database.
- **Built an interactive single-page dashboard** (HTML5/TailwindCSS) with a real-time request burst simulator to visually demonstrate HTTP 200 vs HTTP 429 responses and live header metrics (`X-RateLimit-Remaining`).
- **Developed a comprehensive test suite with JUnit 5 & Mockito**, featuring a 20-thread concurrent stress test using `CountDownLatch` to empirically prove thread safety under heavy lock contention.

---

## 🏗️ High-Level Architecture & Request Flow

```
                               ┌──────────────────────────────────┐
                               │       Incoming HTTP Request      │
                               └─────────────────┬────────────────┘
                                                 │
                                                 ▼
                               ┌──────────────────────────────────┐
                               │         RateLimitFilter          │ (Spring Servlet Filter @Order(1))
                               └─────────────────┬────────────────┘
                                                 │
                                                 ▼
                               ┌──────────────────────────────────┐
                               │         RateLimitService         │
                               └────────┬─────────────────┬───────┘
                                        │                 │
             ┌──────────────────────────┘                 └──────────────────────────┐
             ▼                                                                       ▼
┌───────────────────────────┐                                           ┌───────────────────────────┐
│     RateLimiterCache      │ (ConcurrentHashMap)                       │   RateLimitEventPublisher │ (Observer Pattern)
└────────────┬──────────────┘                                           └────────────┬──────────────┘
             │                                                                       │
             ▼                                                                       ▼
┌───────────────────────────┐                                           ┌───────────────────────────┐
│    RateLimiter Strategy   │                                           │  RateLimitEventListener   │
│  (Token Bucket / Sliding) │                                           └────────────┬──────────────┘
└───────────────────────────┘                                                        │
                                                                                     ▼
                                                                        ┌───────────────────────────┐
                                                                        │    ViolationRepository    │ (H2 Audit Log DB)
                                                                        └───────────────────────────┘
```

### Detailed Request Flow:
1. **Request Interception:** Incoming HTTP requests hit `RateLimitFilter` (ordered first in the filter chain). Static assets (`.html`, `.css`, `.js`) and management endpoints are bypassed.
2. **Client Identification:** Client IP address is extracted from `request.getRemoteAddr()` or `X-Forwarded-For`.
3. **Pattern Matching:** `RateLimitService` checks the client URI against active database path configurations (e.g., `/api/**`, `/api/premium/**`).
4. **Cache Lookup & Instantiation:** A thread-safe `ConcurrentHashMap` holds rate limiter instances per path. If missing, `RateLimiterFactory` instantiates the configured strategy.
5. **Rate Evaluation:** `rateLimiter.allowRequest(clientIp)` evaluates the client's current quota.
6. **Decision Execution:**
   - **Allowed (HTTP 200 OK):** Appends `X-RateLimit-Remaining` and `X-RateLimit-Algorithm` response headers and passes request down the filter chain.
   - **Exceeded (HTTP 429 Too Many Requests):** Asynchronously publishes `RateLimitEvent` to log the violation, returns a JSON error response, and halts downstream request processing.

---

## 💡 Rate Limiting Algorithms Deep-Dive

### 1. Token Bucket Algorithm (`TokenBucketRateLimiter`)
- **Concept:** A bucket holds a maximum capacity $N$ of tokens. Tokens are continuously refilled at a constant rate $R = \text{maxRequests} / \text{windowSeconds}$. Each request consumes 1 token.
- **Mathematical Refill Logic:**
  $$\text{tokensToAdd} = (\text{currentTime} - \text{lastRefillTime}) \times R$$
  $$\text{tokens} = \min(\text{maxCapacity}, \text{tokens} + \text{tokensToAdd})$$
- **Pros:** Smoothly handles traffic bursts up to bucket capacity while enforcing a strict long-term average rate.
- **Time Complexity:** $\mathcal{O}(1)$ lookup & arithmetic update.
- **Space Complexity:** $\mathcal{O}(C)$ where $C$ is the number of active clients.

---

### 2. Sliding Window Log Algorithm (`SlidingWindowRateLimiter`)
- **Concept:** Maintains a historical log of request timestamps (`Deque<Long>`) for each client.
- **Eviction Logic:** When a request arrives at timestamp $T$, all timestamps older than $(T - \text{windowMillis})$ are evicted from the front of the deque. If `deque.size() < maxRequests`, $T$ is appended.
- **Pros:** 100% accurate; completely eliminates boundary burst vulnerabilities.
- **Cons:** Higher memory footprint due to storing individual request timestamps.
- **Time Complexity:** $\mathcal{O}(K)$ cleanup where $K$ is the count of expired timestamps.
- **Space Complexity:** $\mathcal{O}(C \times N)$ where $N$ is the number of requests per window.

---

### 3. Fixed Window Counter Algorithm (`FixedWindowRateLimiter`)
- **Concept:** Time is partitioned into static windows of duration $W$. An atomic counter tracks requests within the current window.
- **Reset Logic:** If $(\text{currentTime} - \text{windowStart}) \ge W$, the window resets and counter sets to 1. Otherwise, counter increments.
- **Pros:** Lowest memory utilization ($\mathcal{O}(1)$ counter per client).
- **Cons:** Boundary edge-case: A client can send $2N$ requests across a window boundary (e.g., $N$ requests at second 59 and $N$ requests at second 61).
- **Time Complexity:** $\mathcal{O}(1)$.

---

## 🛠️ Software Design Patterns Used

| Pattern | Class / Location | Purpose & Benefit |
|---|---|---|
| **Strategy Pattern** | `RateLimiter` interface & implementations (`TokenBucketRateLimiter`, `SlidingWindowRateLimiter`, `FixedWindowRateLimiter`) | Encapsulates rate limiting algorithms behind a unified contract (`allowRequest`, `getRemainingRequests`). Allows interchangeable algorithm selection. |
| **Factory Pattern** | `RateLimiterFactory` | Decouples object creation from business logic. Instantiates the correct `RateLimiter` strategy based on string parameters or DB config. |
| **Observer Pattern** | `ApplicationEventPublisher`, `RateLimitEvent`, `RateLimitEventListener` | Decouples HTTP request execution from audit logging. When a limit is violated, an event is published asynchronously, ensuring zero impact on request latency. |
| **Singleton / Cache** | `RateLimitService.rateLimiterCache` | Employs `ConcurrentHashMap` to reuse `RateLimiter` strategy objects per path pattern rather than re-instantiating on every HTTP request. |
| **Servlet Filter** | `RateLimitFilter` (`jakarta.servlet.Filter`) | Provides clean separation of concerns by intercepting HTTP requests before they reach REST controllers. |
| **Builder Pattern** | Lombok `@Builder` on `RateLimitConfig`, `RateLimitResponse` | Simplifies immutable object creation with readable fluid API syntax. |

---

## 🔒 Concurrency & Thread Safety Model

Web applications in Spring Boot process incoming HTTP requests concurrently using Tomcat worker threads. Concurrent state mutations without proper synchronization lead to **race conditions**, such as over-granting request tokens or corrupted timestamp logs.

RateShield guarantees thread safety through a multi-layered concurrency architecture:

1. **`ConcurrentHashMap<String, RateLimiter>`:** Stores per-path rate limiters and per-client bucket states. Guarantees atomic lock-free reads and safe concurrent map modifications via `computeIfAbsent()`.
2. **Fine-Grained Object-Level Locking (`synchronized (clientState)`):** Locks are placed **only on individual client buckets/deques**, NOT on the entire service or global map. This minimizes lock contention, allowing concurrent requests from different clients to process completely in parallel without blocking each other.
3. **Atomic Primitives (`AtomicInteger`):** Used in counter-based algorithms to perform lock-free thread-safe increments (`incrementAndGet()`).

---

## 📂 Complete Directory Structure

```
rateshield/
├── pom.xml                                   # Maven dependencies & build config
├── Dockerfile                                # Multi-stage Docker container build definition
├── README.md                                 # Complete documentation & interview guide
├── src/
│   ├── main/
│   │   ├── java/com/rateshield/
│   │   │   ├── RateShieldApplication.java    # Spring Boot Main Entry Point
│   │   │   │
│   │   │   ├── algorithm/                    # Core DSA & Strategy Pattern
│   │   │   │   ├── RateLimiter.java          # Strategy Interface
│   │   │   │   ├── TokenBucketRateLimiter.java
│   │   │   │   ├── SlidingWindowRateLimiter.java
│   │   │   │   ├── FixedWindowRateLimiter.java
│   │   │   │   └── RateLimiterFactory.java   # Factory Pattern
│   │   │   │
│   │   │   ├── model/                        # JPA Entities & DTOs
│   │   │   │   ├── RateLimitConfig.java      # Rule Config Entity
│   │   │   │   ├── RateLimitViolation.java   # Audit Log Entity
│   │   │   │   └── RateLimitResponse.java    # Response Payload DTO
│   │   │   │
│   │   │   ├── repository/                   # Spring Data JPA Repositories
│   │   │   │   ├── RateLimitConfigRepository.java
│   │   │   │   └── ViolationRepository.java
│   │   │   │
│   │   │   ├── service/                      # Business Logic & Cache
│   │   │   │   ├── RateLimitService.java
│   │   │   │   └── ViolationService.java
│   │   │   │
│   │   │   ├── filter/                       # Request Interceptor
│   │   │   │   └── RateLimitFilter.java      # Servlet Filter Order(1)
│   │   │   │
│   │   │   ├── controller/                   # REST APIs
│   │   │   │   ├── ConfigController.java     # Rule Management CRUD
│   │   │   │   └── DashboardController.java  # Analytics Stats API
│   │   │   │
│   │   │   ├── observer/                     # Observer Event Pattern
│   │   │   │   ├── RateLimitEvent.java
│   │   │   │   └── RateLimitEventListener.java
│   │   │   │
│   │   │   └── exception/                    # Global Error Handling
│   │   │       ├── RateLimitExceededException.java
│   │   │       └── GlobalExceptionHandler.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml               # Application Configuration
│   │       ├── data.sql                      # Seed Data (Default Rules)
│   │       └── static/
│   │           └── index.html                # Interactive Frontend Dashboard
│   │
│   └── test/
│       └── java/com/rateshield/
│           ├── algorithm/                    # Algorithm Unit Tests
│           │   ├── TokenBucketRateLimiterTest.java
│           │   ├── SlidingWindowRateLimiterTest.java
│           │   └── FixedWindowRateLimiterTest.java
│           ├── service/
│           │   └── RateLimitServiceTest.java # Service Logic Tests
│           └── integration/
│               └── RateLimitIntegrationTest.java # ⭐ Multi-threaded Stress Test
```

---

## 📡 REST API Documentation

### 1. Rate Limit Configuration Endpoints (`/management/config`)

| Method | Endpoint | Description | Sample Payload |
|---|---|---|---|
| `GET` | `/management/config/` | Retrieve all active rate limit rules | N/A |
| `GET` | `/management/config/{id}` | Get specific rule by ID | N/A |
| `POST` | `/management/config/` | Create a new rate limit rule | `{"pathPattern":"/api/v1/**","algorithm":"TOKEN_BUCKET","maxRequests":20,"windowSeconds":60,"enabled":true}` |
| `PUT` | `/management/config/{id}` | Update an existing rule | `{"pathPattern":"/api/v1/**","algorithm":"SLIDING_WINDOW","maxRequests":50,"windowSeconds":60,"enabled":true}` |
| `DELETE` | `/management/config/{id}` | Delete a rule by ID | N/A |

### 2. Analytics Dashboard Endpoints (`/management/dashboard`)

| Method | Endpoint | Description | Response Example |
|---|---|---|---|
| `GET` | `/management/dashboard/stats` | Get rule count, 24h violations, top violators | `{"totalConfigs":2,"totalViolationsLast24h":5,"topViolators":[["127.0.0.1",5]]}` |
| `GET` | `/management/dashboard/violations/{ip}` | Get violation audit trail for specific IP | `[{"id":1,"clientIp":"127.0.0.1","requestPath":"/api/test","algorithm":"TOKEN_BUCKET"}]` |

---

## 🧪 Testing & Verification

RateShield includes a thorough test suite containing **23 automated tests** written in **JUnit 5** and **Mockito**.

### Key Tests:
- **Unit Tests:** Verify mathematical token refills, window log evictions, fixed window resets, and edge cases across all 3 algorithm classes.
- **Service Tests:** Mock database repositories and verify that `RateLimitEvent` is published when limits are exceeded.
- **Multi-Threaded Stress Test (`shouldHandleConcurrentRequestsSafely`):** 
  Utilizes an `ExecutorService` with 20 threads synchronized by a `CountDownLatch`. Fires 20 simultaneous requests against a Token Bucket with capacity 10. Asserts that **exactly 10 requests pass and 10 fail**, proving thread safety under severe lock contention.

### Run Tests Command:
```bash
mvn clean test
```

---

## 💻 How to Run Locally

### Option A: Run via Maven
```bash
mvn spring-boot:run
```

### Option B: Run Standalone Executable JAR
```bash
mvn clean package -DskipTests
java -jar target/rateshield-1.0.0.jar
```

Access the UI at **`http://localhost:8080`** and H2 Console at **`http://localhost:8080/h2-console`**.

---

## ☁️ How to Deploy to Cloud

### Deploy on Render / Railway / Koyeb via Docker
1. Push repository to GitHub: `https://github.com/rithika20252024/ratelimiting`
2. Connect your repository to **Render** or **Railway**.
3. Select **Docker** environment. The included `Dockerfile` will automatically build the Maven artifact and run the lightweight Alpine container on port 8080.

---

## 🎓 Recruiter & SDE Interview Cheat Sheet

### Q1: "Why did you build an API Rate Limiter?"
> **Answer:** "I wanted to build a core backend infrastructure project rather than a typical CRUD application. An API Rate Limiter solves critical microservice architecture problems like DDoS protection, API monetization tiers, and resource exhaustion. It allowed me to demonstrate data structure selection, multithreaded concurrency, and design patterns."

### Q2: "How did you ensure high performance and thread safety?"
> **Answer:** "I used `ConcurrentHashMap` to store rate limiters per path and client IP. To prevent thread contention from slowing down the gateway, I avoided synchronizing whole methods or global maps. Instead, I applied fine-grained synchronization scoped exclusively to the specific client's state bucket. This allows requests from Client A and Client B to execute concurrently without blocking each other."

### Q3: "How would you scale RateShield from single-node to a distributed system?"
> **Answer:** "Currently, rate state is stored in-memory per gateway instance. To scale horizontally across multiple gateway nodes behind a load balancer, I would replace the in-memory state with a **centralized Redis cluster**. I would execute atomic rate limiting checks in Redis using **Lua scripts** for Token Bucket and Sliding Window algorithms to eliminate network round-trip race conditions."

### Q4: "What is the difference between Token Bucket and Sliding Window Log?"
> **Answer:** "Token Bucket refills tokens continuously using time math, allowing bursts up to bucket capacity $N$ with an $\mathcal{O}(1)$ time complexity and minimal memory. Sliding Window Log keeps exact timestamps of every request in a Deque to provide 100% boundary accuracy, but requires $\mathcal{O}(N)$ memory per client."
