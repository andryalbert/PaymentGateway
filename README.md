# FinTech Payment Platform — Kafka Microservices

A production-ready, distributed, and scalable payment processing platform built with microservices architecture, Apache Kafka, and event-driven design.

## 🏗️ Architecture Overview

This platform implements a complete FinTech payment gateway with the following components:

### Microservices Architecture

```
CLIENTS (Web/Mobile/API)
           ↓
    API Gateway (Nginx)
           ↓
Authentication (Keycloak)
           ↓
    Payment Service (Spring Boot)
           ↓
    PostgreSQL + Outbox Pattern
           ↓
    Apache Kafka Cluster
           ↓
    ┌──────┴──────┬──────────┬──────────┐
    ↓             ↓          ↓          ↓
Fraud Service  Ledger Service  Notification Service
    ↓             ↓          ↓
PostgreSQL    PostgreSQL  PostgreSQL
    ↓             ↓          ↓
    └──────┬──────┴──────────┘
           ↓
    Settlement Service
           ↓
    External PSP/Bank
           ↓
    Reconciliation
```

## 🚀 Features

### Core Payment Features
- **Payment Creation**: Secure payment processing with validation
- **Idempotence**: Prevent duplicate payments using idempotency keys
- **Fraud Detection**: Real-time fraud analysis with configurable rules
- **Financial Ledger**: Double-entry accounting for all transactions
- **Settlement**: Automated batch settlement to external PSPs
- **Notifications**: Email/SMS notifications for payment events
- **Reporting**: Daily aggregations and comprehensive analytics
- **Audit Trail**: Complete transaction history and audit logs

### Technical Features
- **Event-Driven Architecture**: Apache Kafka for asynchronous communication
- **Outbox Pattern**: Reliable event publishing with guaranteed delivery
- **Microservices**: Independent, scalable services
- **API Gateway**: Nginx with rate limiting and routing
- **Authentication**: Keycloak for IAM and security
- **Observability**: Prometheus metrics and Grafana dashboards
- **High Availability**: Health checks and automatic restarts
- **CI/CD**: Automated build, test, and deployment pipeline

## 📋 Services

### 1. Payment Service (Port 8081)
- Core payment processing logic
- Idempotency handling
- Outbox pattern implementation
- Payment lifecycle management

### 2. Fraud Detection Service (Port 8082)
- Real-time fraud analysis
- Configurable fraud rules
- Risk scoring algorithms
- Velocity checks and geolocation validation

### 3. Ledger Service (Port 8083)
- Double-entry accounting
- Account management
- Transaction recording
- Balance tracking

### 4. Notification Service (Port 8084)
- Email notifications
- SMS integration
- Payment status updates
- Multi-channel delivery

### 5. Settlement Service (Port 8085)
- Batch settlement processing
- PSP integration
- T+2 settlement cycle
- Reconciliation support

### 6. Reporting Service (Port 8086)
- Daily aggregations
- Merchant analytics
- Transaction reporting
- Business intelligence

## 🛠️ Technology Stack

### Backend
- **Java 21**: Modern Java with enhanced features
- **Spring Boot 3.2.0**: Microservices framework
- **Spring Kafka**: Kafka integration
- **Spring Data JPA**: Database access
- **PostgreSQL 16**: Relational databases
- **Lombok**: Boilerplate reduction

### Infrastructure
- **Apache Kafka 7.5**: Event streaming platform
- **Zookeeper**: Kafka coordination
- **Nginx**: API Gateway and load balancing
- **Keycloak 23**: Identity and Access Management
- **Docker**: Containerization
- **Docker Compose**: Orchestration

### Monitoring
- **Prometheus**: Metrics collection
- **Grafana**: Visualization and dashboards
- **Spring Actuator**: Application monitoring

### Frontend
- **Angular 21**: Modern web framework
- **TypeScript**: Type-safe JavaScript
- **Nginx**: Static file serving

## 📋 Prerequisites

- Docker and Docker Compose
- Java 21 (for local development)
- Node.js 20 (for frontend development)
- Maven (for backend development)

## 🚀 Quick Start

### Using Docker Compose (Recommended)

1. Clone the repository:
```bash
git clone <repository-url>
cd PaymentGateway
```

2. Create environment file:
```bash
cp .env.example .env
# Edit .env with your configuration
```

3. Start all services:
```bash
docker-compose up -d
```

4. Access the platform:
- **Frontend**: http://localhost:3000
- **API Gateway**: http://localhost
- **Keycloak**: http://localhost:8080
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3001 (admin/admin123)

### Keycloak Credentials

**Default Admin Credentials:**
- **Username**: `admin`
- **Password**: `admin123`

**Keycloak URLs:**
- **Home Page**: http://localhost:8080/
- **Admin Console**: http://localhost:8080/admin/
- **Master Realm**: http://localhost:8080/realms/master/

**Database Credentials:**
- **Database**: keycloak
- **Username**: keycloakuser
- **Password**: keycloakpass

### Service Endpoints

- **Payment Service**: http://localhost:8081/api/v1/payments
- **Fraud Service**: http://localhost:8082/api/v1/fraud
- **Ledger Service**: http://localhost:8083/api/v1/ledger
- **Notification Service**: http://localhost:8084/api/v1/notifications
- **Settlement Service**: http://localhost:8085/api/v1/settlements
- **Reporting Service**: http://localhost:8086/api/v1/reports

## 📡 API Documentation

### Payment Service

#### Create Payment
```http
POST /api/v1/payments
Content-Type: application/json

{
  "cardNumber": "1234567890123456",
  "cardHolder": "John Doe",
  "expiryDate": "12/25",
  "cvv": "123",
  "amount": 100.00,
  "currency": "USD",
  "merchantId": "MERCHANT-001",
  "customerId": "CUSTOMER-001",
  "description": "Test payment",
  "ipAddress": "192.168.1.1",
  "userAgent": "Mozilla/5.0",
  "billingAddress": "123 Main St",
  "billingCity": "New York",
  "billingCountry": "US",
  "billingZip": "10001",
  "idempotencyKey": "unique-key-123"
}
```

#### Get Payment
```http
GET /api/v1/payments/{paymentId}
```

#### Refund Payment
```http
POST /api/v1/payments/{paymentId}/refund
```

### Other Services

Each service follows similar REST patterns with health check endpoints at `/health`.

## 🔄 Kafka Topics

The platform uses the following Kafka topics:

- **payment-events**: Main payment lifecycle events
- **fraud-response**: Fraud detection results
- **settlement-response**: Settlement completion notifications
- **settlement-events**: Settlement batch events

## 🔒 Security Features

- **Authentication**: Keycloak OAuth 2.0/OIDC
- **Authorization**: Role-based access control
- **Data Encryption**: Card number masking in database
- **Input Validation**: Comprehensive validation on all endpoints
- **Rate Limiting**: API Gateway rate limiting (10 req/s)
- **CORS**: Configured cross-origin resource sharing
- **SQL Injection Prevention**: JPA/Hibernate parameterized queries

## 📊 Monitoring & Observability

### Prometheus Metrics

All services expose metrics at `/actuator/prometheus`:

- JVM metrics
- HTTP request metrics
- Custom business metrics
- Kafka consumer/producer metrics

### Grafana Dashboards

Pre-configured dashboards for:
- Service health
- Payment throughput
- Fraud detection rates
- Settlement status
- System resources

## 🧪 Testing

### Backend Tests
```bash
cd payment-service
mvn test
```

### Integration Tests
```bash
docker-compose -f docker-compose.test.yml up --abort-on-container-exit
```

## 🚀 Deployment

### Production Deployment

1. Build all service images:
```bash
docker-compose build
```

2. Push to registry:
```bash
docker-compose push
```

3. Deploy on production server:
```bash
docker-compose -f docker-compose.prod.yml up -d
```

### CI/CD Pipeline

The GitHub Actions workflow automatically:
- Builds and tests each microservice
- Creates Docker images
- Pushes to Docker Hub (on main branch)
- Deploys to production (on main branch)

## 🗄️ Database Schema

Each microservice has its own PostgreSQL database:

- **payment-db**: Payments and outbox events
- **fraud-db**: Fraud checks and rules
- **ledger-db**: Accounts and ledger entries
- **notification-db**: Notifications and delivery status
- **settlement-db**: Settlement batches and items
- **reporting-db**: Payment reports and aggregations

## 📝 Configuration

### Environment Variables

Key environment variables (see `.env.example`):

- `SMTP_USERNAME`: Email service credentials
- `SMTP_PASSWORD`: Email service password
- `PSP_API_KEY`: Payment Service Provider API key
- `DOCKER_USERNAME`: Docker Hub username
- `DOCKER_PASSWORD`: Docker Hub password

### Service Configuration

Each service has its own `application.yml` with:
- Database connection settings
- Kafka configuration
- Service-specific settings
- Logging configuration

## 🔧 Development

### Local Development Setup

1. Start infrastructure:
```bash
docker-compose up -d zookeeper kafka postgres keycloak
```

2. Run individual services:
```bash
cd payment-service
mvn spring-boot:run
```

3. Run frontend:
```bash
cd frontend
npm install
npm start
```

## 🚀 Service Startup Order

### Docker Compose (Full Stack)

When using Docker Compose with all services enabled, the startup order is automatically managed by dependencies:

```bash
docker-compose up -d
```

The services start in this order:
1. **Infrastructure Layer**
   - zookeeper (Kafka coordination)
   - kafka (Event streaming)
   - All PostgreSQL databases (payment-db, fraud-db, ledger-db, notification-db, settlement-db, reporting-db, keycloak-db)
   - keycloak (Authentication)

2. **Monitoring Layer**
   - prometheus
   - grafana

3. **Microservices Layer** (start in parallel once dependencies are healthy)
   - payment-service (port 8081)
   - fraud-service (port 8082)
   - ledger-service (port 8083)
   - notification-service (port 8084)
   - settlement-service (port 8085)
   - reporting-service (port 8086)

4. **Gateway & Frontend**
   - api-gateway (Nginx, ports 80/443)
   - frontend (port 3000)

### Local Development (Manual Startup)

If running services locally without Docker Compose for microservices:

1. **Start Infrastructure First**
```bash
docker-compose up -d zookeeper kafka payment-db fraud-db ledger-db notification-db settlement-db reporting-db keycloak-db keycloak prometheus grafana frontend
```

2. **Start Microservices in Order** (each in separate terminal)
```bash
# Terminal 1
cd payment-service
mvn spring-boot:run

# Terminal 2
cd fraud-service
mvn spring-boot:run

# Terminal 3
cd ledger-service
mvn spring-boot:run

# Terminal 4
cd notification-service
mvn spring-boot:run

# Terminal 5
cd settlement-service
mvn spring-boot:run

# Terminal 6
cd reporting-service
mvn spring-boot:run
```

**Important Notes:**
- All microservices depend on Kafka (localhost:9092), so Kafka must be running before any Spring Boot service
- Each service waits for its database to be healthy before starting
- The API Gateway depends on all microservices being available

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a Pull Request

## 📄 License

This project is licensed under the MIT License.

## 👥 Authors

- Andry Albert

## 🙏 Acknowledgments

- Spring Boot team
- Apache Kafka community
- Keycloak project
- Docker community
- Open source FinTech community
