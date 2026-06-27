<div align="center">

# 🤖 AI Resume Screener API

### AI-Powered Resume Analysis & Job Description Matching Engine

[![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-blue?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![Groq](https://img.shields.io/badge/AI-Groq%20LLaMA%203-purple?style=flat-square)](https://groq.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)]()

</div>

---

## 📖 Overview

**AI Resume Screener API** is a backend system that automates resume screening using Large Language Models. It extracts text from uploaded PDF resumes, leverages **Groq's LLaMA 3** model via **Spring AI** to identify skills, experience, and education, and intelligently scores candidates against job descriptions — complete with category-wise breakdowns and hiring recommendations.

Built with a clean, layered Spring Boot architecture, secured with JWT authentication, and fully documented via Swagger.

---

## ✨ Key Features

| Feature | Description |
|---|---|
| 🔐 **JWT Authentication** | Secure register/login flow with access & refresh tokens |
| 📄 **PDF Resume Parsing** | Extracts raw text from resumes using Apache PDFBox |
| 🧠 **AI-Powered Analysis** | LLM extracts structured skills, experience, education & summary |
| 🎯 **JD Matching & Scoring** | Scores resumes 0–100 vs job descriptions with category breakdowns |
| 📊 **Smart Dashboards** | Paginated, score-filtered views of resumes & screenings |
| 👮 **Role-Based Access** | Separate USER and ADMIN privilege levels |
| 📑 **Interactive API Docs** | Full Swagger / OpenAPI 3 documentation |

---

## 🛠️ Tech Stack

<div align="center">

| Layer | Technology |
|:---:|:---:|
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.2 |
| **Security** | Spring Security + JWT |
| **Database** | PostgreSQL + Spring Data JPA |
| **AI Engine** | Spring AI + Groq (LLaMA 3 - 8B) |
| **PDF Parsing** | Apache PDFBox |
| **Documentation** | SpringDoc OpenAPI / Swagger UI |
| **Build Tool** | Maven |

</div>

---

## 🏗️ Architecture

**Request Flow:**

`Controller → Service → Repository → PostgreSQL`

**AI Flow:**

`Service → Groq LLM (via Spring AI) → Structured JSON Response`

- ✅ Clean **DTO** pattern for all requests/responses
- ✅ **Global Exception Handler** with custom exception types
- ✅ **Pagination** on all list endpoints
- ✅ Stateless **JWT** security layer

---

## 📡 API Reference

### 🔑 Authentication
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Register a new user |
| `POST` | `/api/auth/login` | Login and receive JWT tokens |
| `POST` | `/api/auth/refresh` | Refresh access token |

### 📄 Resume & Screening
| Method | Endpoint | Auth | Description |
|---|---|:---:|---|
| `POST` | `/api/resumes/upload` | 🔒 | Upload a resume PDF |
| `POST` | `/api/resumes/{id}/analyze` | 🔒 | AI-extract skills, experience & education |
| `POST` | `/api/resumes/{id}/screen` | 🔒 | Score resume against a job description |
| `GET` | `/api/resumes/{id}/screenings` | 🔒 | Paginated screening history |

### 📊 Dashboard & Admin
| Method | Endpoint | Auth | Description |
|---|---|:---:|---|
| `GET` | `/api/dashboard/resumes?minScore=70` | 🔒 | Filtered resume dashboard |
| `GET` | `/api/dashboard/screenings?minScore=70` | 🔒 | Filtered screening dashboard |
| `GET` | `/api/admin/users` | 👮 | List all users (Admin only) |
| `GET` | `/api/admin/users/{id}/resumes` | 👮 | View any user's resumes (Admin only) |

---

## 🚀 Getting Started

### Prerequisites
- ☑️ Java 17+
- ☑️ Maven
- ☑️ PostgreSQL
- ☑️ Free [Groq API Key](https://console.groq.com)

### Installation

**1. Clone the repository**

    git clone https://github.com/yashpratap-dev/ai-resume-screener.git
    cd ai-resume-screener

**2. Create the database**

    CREATE DATABASE resume_screener;

**3. Configure environment**

Copy `application.properties.example` → `application.properties` and update:

    spring.datasource.username=your_db_username
    spring.datasource.password=your_db_password
    spring.ai.openai.api-key=your_groq_api_key
    app.jwt.secret=your_jwt_secret

**4. Run the application**

    mvn spring-boot:run

**5. Explore the API**

Open Swagger UI → **http://localhost:8080/swagger-ui.html**

---

## 🔒 Security Highlights

- 🔑 Passwords hashed with **BCrypt**
- 🛡️ Stateless **JWT-based** authentication (access + refresh tokens)
- 👥 **Role-based authorization** (USER / ADMIN)
- 🚫 Credentials excluded from version control via `.gitignore`

---

## 📌 Roadmap

- [ ] Redis caching for repeated AI analysis
- [ ] Async resume processing with job queues
- [ ] React-based frontend dashboard
- [ ] Bulk resume upload & batch screening

---

<div align="center">

### 👨‍💻 Author

**Yash Pratap Singh**

[![GitHub](https://img.shields.io/badge/GitHub-yashpratap--dev-181717?style=flat-square&logo=github)](https://github.com/yashpratap-dev)

</div>
