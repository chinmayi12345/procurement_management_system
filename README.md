# 📦 Procurement Management System

A full-stack Procurement Management System built with **React (Vite)** on the frontend and **Spring Boot (Java 17 / Maven)** on the backend with **MySQL** database integration and JWT-based authentication.

---

## 🚀 Features

- **Authentication & Authorization**: Secure JWT-based registration and login with role-based access control (Admin / User / Supplier actions).
- **Purchase Request Workflow**:
  - Users create and submit purchase requisitions.
  - Admins review, approve, or reject pending requests.
- **Supplier & Payment Integration**:
  - Supplier console sends payment requests and quotes.
  - Users pay and record transactions.
  - CSV export for payment history.
- **Shipment Tracking**:
  - Suppliers update shipment and tracking numbers.
  - Users view full tracking history and status timeline.
- **Supplier Rating System**:
  - Users submit performance ratings and reviews for suppliers.
- **Product & Inventory Management**:
  - Preloaded product catalog with matching dynamic imagery.
  - Real-time stock and status tracking.

---

## 🛠️ Tech Stack

### Frontend
- **Framework**: React 18 (Vite)
- **Routing**: React Router
- **HTTP Client**: Axios
- **Icons**: Lucide React
- **Styling**: Vanilla CSS (Tailored Modern Design System)

### Backend
- **Framework**: Spring Boot 3
- **Language**: Java 17+
- **Build Tool**: Maven
- **Database**: MySQL
- **ORM**: Spring Data JPA / Hibernate
- **Security**: Spring Security + JWT
- **Email**: Spring Mail (SMTP)

---

## 📂 Project Structure

```text
procurement-system/
├── backend/                   # Spring Boot backend application
│   ├── src/main/java/         # Controllers, Services, Models, Repositories, Security
│   ├── src/main/resources/    # application.yml, DB migrations/scripts
│   ├── pom.xml                # Maven dependencies
│   └── database-*.sql         # Database setup scripts
├── frontend/                  # React Vite frontend application
│   ├── src/                   # Components, Contexts, Pages, Services
│   ├── public/                # Static assets & product images
│   ├── package.json           # Frontend dependencies
│   └── vite.config.js         # Vite configuration
├── .gitignore                 # Git ignore rules for Maven & Node.js
└── README.md                  # Project documentation
```

---

## ⚡ Getting Started

### 1. Prerequisites
- **Node.js** (v18 or higher)
- **Java Development Kit (JDK)** 17 or higher
- **Maven** (or use included `mvnw`)
- **MySQL Server** (running on port 3306)

---

### 2. Database Setup
1. Start your MySQL service.
2. Create the database:
   ```sql
   CREATE DATABASE procurement_db;
   ```
3. Run the SQL scripts in `backend/` if needed for custom seed data.

---

### 3. Backend Setup

1. Navigate to the backend directory:
   ```bash
   cd backend
   ```
2. Update database and mail settings in [application.yml](file:///e:/procurement-system/backend/src/main/resources/application.yml) (or pass them as environment variables):
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/procurement_db
       username: root
       password: your_mysql_password
   ```
3. Build and run the backend:
   ```bash
   ./mvnw spring-boot:run
   ```
   *Backend server will start at: `http://localhost:8081`*

---

### 4. Frontend Setup

1. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. (Optional) Configure environment:
   Create a `.env` file in `frontend/` (based on `.env.example`):
   ```env
   VITE_API_URL=http://localhost:8081/api
   ```
4. Start the development server:
   ```bash
   npm run dev
   ```
   *Frontend application will open at: `http://localhost:5173`*

---

## 🔒 Environment Variables & Security

Make sure not to commit sensitive passwords or credentials to version control. The following environment variables are supported by the backend:

| Variable | Description | Default |
| :--- | :--- | :--- |
| `SPRING_DATASOURCE_USERNAME` | MySQL database username | `root` |
| `SPRING_DATASOURCE_PASSWORD` | MySQL database password | `root` |
| `MAIL_USERNAME` | SMTP Email username | Optional |
| `MAIL_PASSWORD` | SMTP App password | Optional |
| `JWT_SECRET` | HMAC Secret key for JWT | Configured default |

---

## 📄 License
This project is open source and available under the [MIT License](LICENSE).
