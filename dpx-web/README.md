# DPX – Courier Delivery Management System

**Course Paper DAT1080P** | Java / Spring Boot Web Application

A fully functional web-based CRUD application for managing courier deliveries.

---

## Features

- **Create** new deliveries (ID, sender, recipient, address, weight)
- **Read** – list all deliveries, search by ID / name / address
- **Update** – edit any delivery details or status
- **Delete** – remove a delivery with confirmation
- **Statistics** dashboard (Total, Pending, In Transit, Delivered)
- Delivery status lifecycle: `Pending → In Transit → Delivered → Cancelled`

---

## Requirements

| Tool | Version |
|------|---------|
| Java JDK | 17 or higher |
| Maven | 3.6+ (or use IntelliJ's built-in) |

---

## How to Run Locally

### Option 1 – IntelliJ IDEA (Recommended)

1. Open IntelliJ IDEA
2. `File → Open` → select the `dpx-delivery` folder
3. Wait for Maven to download dependencies (first run only)
4. Open `src/main/java/com/dpx/DpxApplication.java`
5. Click the **▶ Run** button
6. Open your browser → **http://localhost:8080**

### Option 2 – Command Line

```bash
# In the project root folder (where pom.xml is):
mvn spring-boot:run
```

Then open: **http://localhost:8080**

---

## REST API Endpoints

| Method | URL | Description |
|--------|-----|-------------|
| GET | `/api/deliveries` | List all deliveries |
| GET | `/api/deliveries?search=john` | Search deliveries |
| GET | `/api/deliveries/{id}` | Get delivery by ID |
| POST | `/api/deliveries` | Create new delivery |
| PUT | `/api/deliveries/{id}` | Update delivery |
| DELETE | `/api/deliveries/{id}` | Delete delivery |
| GET | `/api/deliveries/stats` | Get statistics |

---

## Project Structure

```
dpx-delivery/
├── pom.xml
└── src/main/
    ├── java/com/dpx/
    │   ├── DpxApplication.java          # Entry point
    │   ├── model/Delivery.java          # Data model
    │   ├── repository/DeliveryRepository.java   # In-memory store
    │   └── controller/DeliveryController.java   # REST API
    └── resources/
        ├── application.properties
        └── static/index.html            # Web UI
```

---

## Technologies

- **Java 17**
- **Spring Boot 3.2** (embedded Tomcat)
- **Bootstrap 5** (frontend UI)
- **Maven** (build tool)
- No database required — data stored in memory

---

## Author

[Student Name] | DAT1080P Course Paper 3  
Supervisor: Jānis Pekša, Ph.D., Associate Professor
