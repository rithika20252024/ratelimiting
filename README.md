# 🛡️ RateShield — Distributed-Ready API Rate Limiter & Gateway

RateShield is an enterprise-grade API Rate Limiter built with Java 17 and Spring Boot. It acts as an API gateway middleware that intercepts incoming HTTP requests and enforces rate limits to protect backend services against spam, DDoS attacks, and resource starvation.

---

## 🚀 Key Highlights & Resume Bullet Points

> **Add these bullet points directly to your resume under Projects:**

- **Engineered an API Rate Limiter & Gateway in Java 17 and Spring Boot**, protecting API endpoints against request flooding by processing high-throughput traffic and enforcing granular client rate limits.
- **Implemented 3 core rate limiting algorithms** (*Token Bucket, Sliding Window Log, Fixed Window Counter*) using the **Strategy & Factory design patterns**, enabling dynamic runtime switching without downtime.
- **Designed for concurrency & thread safety**, leveraging `ConcurrentHashMap`, atomic operations, and synchronized locking to prevent race conditions during high-concurrency request bursts.
- **Utilized Spring Servlet Filter and Observer Pattern** (`ApplicationEventPublisher`) to intercept incoming requests with low latency and asynchronously log rate limit violations to an H2/JPA audit store.
- **Built an automated test suite with JUnit 5 & Mockito**, featuring a multi-threaded stress test simulating 20+ concurrent threads to empirically prove thread safety under heavy contention.

---

## 🏗️ System Architecture

```
                               ┌──────────────────────────────────┐
                               │       Incoming HTTP Request      │
                               └─────────────────┬────────────────┘
                                                 │
                                                 ▼
                               ┌──────────────────────────────────┐
                               │         RateLimitFilter          │ (Order = 1)
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
│     RateLimiterCache      │ (ConcurrentHashMap)                       │   RateLimitEventPublisher │ (Observer)
└────────────┬──────────────┘                                           └────────────┬──────────────┘
             │                                                                       │
             ▼                                                                       ▼
┌───────────────────────────┐                                           ┌───────────────────────────┐
│    RateLimiter Strategy   │                                           │  RateLimitEventListener   │
│  (TokenBucket / Sliding)  │                                           └────────────┬──────────────┘
└───────────────────────────┘                                                        │
                                                                                     ▼
                                                                        ┌───────────────────────────┐
                                                                        │    ViolationRepository    │ (H2 Database)
                                                                        └───────────────────────────┘
```

---

## 🛠️ Tech Stack & Design Patterns

| Layer | Technology / Pattern | Role |
|---|---|---|
| **Language** | Java 17 | Core programming language |
| **Framework** | Spring Boot 3.2.0 | Dependency Injection, Web, JPA, Events |
| **Database** | H2 In-Memory DB (JPA/Hibernate) | Dynamic rule configuration & violation audit logs |
| **Behavioral Patterns** | **Strategy Pattern** | Standardized `RateLimiter` interface for interchangeable algorithms |
| **Creational Patterns** | **Factory Pattern** | `RateLimiterFactory` instantiates algorithm strategies based on DB configs |
| **Decoupling** | **Observer Pattern** | `ApplicationEventPublisher` & `@EventListener` for asynchronous violation logging |
| **Concurrency** | `ConcurrentHashMap`, `AtomicInteger`, `synchronized` | Thread-safe in-memory rate state tracking per client IP |
| **Testing** | JUnit 5, Mockito, `CountDownLatch` | Unit tests & multi-threaded stress testing |

---

## 💡 Rate Limiting Algorithms Deep-Dive

### 1. Token Bucket Algorithm (`TokenBucketRateLimiter`)
- **How it works:** A bucket has a maximum capacity $N$. Tokens are refilled at a constant rate $R = \text{maxRequests} / \text{windowSeconds}$. Each incoming request consumes 1 token. If no tokens remain, the request is rejected with HTTP 429.
- **Pros:** Handles traffic bursts gracefully while enforcing a smooth average rate limit.
- **Time Complexity:** $\mathcal{O}(1)$ lookup & refill math.

### 2. Sliding Window Log Algorithm (`SlidingWindowRateLimiter`)
- **How it works:** Maintains a timestamp log (`Deque<Long>`) of requests per client. Upon arrival, timestamps older than $(\text{currentTime} - \text{windowMillis})$ are evicted. If the log size is within limits, the current timestamp is appended.
- **Pros:** Extremely accurate; completely eliminates boundary spike issues.
- **Time Complexity:** $\mathcal{O}(K)$ cleanup where $K$ is the number of expired timestamps.

### 3. Fixed Window Counter Algorithm (`FixedWindowRateLimiter`)
- **How it works:** Divides time into fixed windows (e.g., 60-second blocks). A counter tracks request count in the active window. The counter resets when a new window starts.
- **Pros:** Low memory footprint ($\mathcal{O}(1)$ counter per client).
- **Cons:** Boundary condition: A client can send $2N$ requests in a short interval straddling two windows.

---

## ⚡ How to Run & Verify

### Prerequisites
- JDK 17+
- Maven 3.8+

### 1. Run Unit & Integration Tests
```bash
mvn clean test
```

### 2. Launch the Application
```bash
mvn spring-boot:run
```

### 3. Test API Rate Limiting (cURL)
Hit the sample endpoint repeatedly:
```bash
for i in {1..12}; do curl -i http://localhost:8080/api/test; done
```
*After 10 requests within 60 seconds, HTTP 429 Too Many Requests is returned.*

### 4. View H2 Console & Dashboard Stats
- **H2 Console:** `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:rateshield`, User: `sa`, Password: empty)
- **Analytics Dashboard Endpoint:** `http://localhost:8080/management/dashboard/stats`
- **Dynamic Config CRUD:** `http://localhost:8080/management/config/`

---

## 🎓 Recruiter & Interviewer Q&A Prep

### Q1: "Why did you build an API Rate Limiter?"
> **Answer:** "I wanted to build an infrastructure project that solves real-world API gateway challenges like resource starvation and DDoS protection. RateShield allowed me to apply key Software Design Principles—such as the Strategy and Factory design patterns—alongside concurrency management in Java."

### Q2: "How did you ensure thread safety in a multi-threaded web server environment?"
> **Answer:** "In Spring Boot, HTTP requests are handled by Tomcat worker threads concurrently. I used `ConcurrentHashMap` to store rate limiters per client IP/path. For individual client state mutations (like token refill or deque eviction), I used synchronized blocks scoped tightly to the specific client's data structure to minimize lock contention while guaranteeing atomicity."

### Q3: "How do you test concurrent code?"
> **Answer:** "I wrote an integration stress test using Java's `CountDownLatch` and an `ExecutorService` with 20 threads. By holding all 20 threads at a starting line with a `CountDownLatch` and releasing them simultaneously, I simulated maximum thread contention on a bucket with capacity 10. The test asserted that exactly 10 requests succeeded and 10 were rejected, verifying no race conditions occurred."

### Q4: "How would you scale this system from single-node to a distributed system?"
> **Answer:** "Currently, rate state is stored in-memory per node. To scale horizontally across multiple gateway instances, I would migrate the rate-tracking state to **Redis** using **Lua scripts** for atomic Token Bucket or Sliding Window operations, or use Redis `INCR` with key expiration for Fixed Window."
