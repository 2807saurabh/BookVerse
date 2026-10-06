# 📚 BookVerse

A full-stack online bookstore built using **React.js, Spring Boot, MySQL, and Spring Data JPA**.

BookVerse is an e-commerce web application that allows users to browse books, search and filter the catalogue, authenticate securely, and manage their shopping cart. It also provides an admin panel for managing books and catalogue-related operations.

---

## 🚀 Features

### 👤 User Features

- User registration and login
- JWT-based authentication
- Browse books
- Search books
- Filter books by category
- View book information
- Add books to cart
- Manage cart items

### 🔐 Authentication & Authorization

- JWT-based authentication
- Role-based authorization
- Protected backend APIs
- Separate user and admin access

### 🛒 Shopping Cart

- Add books to cart
- View cart items
- Manage cart quantity
- Remove items from cart

### 🛠️ Admin Features

- Admin authentication
- Add books
- Update book details
- Delete books
- Upload book images
- Search books
- Filter books by category
- Manage the book catalogue

---

## 🏗️ Technology Stack

### Frontend

- React.js
- JavaScript
- HTML5
- CSS3

### Backend

- Java
- Spring Boot
- Spring Data JPA
- REST APIs
- Maven

### Database

- MySQL

### Security

- JWT Authentication
- Role-Based Authorization

### Development Tools

- Git
- GitHub
- Postman
- IntelliJ IDEA
- VS Code

---

## 🏛️ Application Architecture

```text
                    ┌───────────────────┐
                    │     React.js      │
                    │    Frontend       │
                    └─────────┬─────────┘
                              │
                              │ REST API
                              ▼
                    ┌───────────────────┐
                    │   Spring Boot     │
                    │     Backend       │
                    ├───────────────────┤
                    │ Controllers       │
                    │ Services           │
                    │ Repositories       │
                    │ Security / JWT     │
                    └─────────┬─────────┘
                              │
                              │ Spring Data JPA
                              ▼
                    ┌───────────────────┐
                    │      MySQL        │
                    │     Database      │
                    └───────────────────┘

🔑 Authentication Flow
User
  │
  ▼
Login / Register
  │
  ▼
Spring Boot Backend
  │
  ▼
Authentication
  │
  ▼
JWT Token
  │
  ▼
Authenticated Requests
  │
  ▼
Role-Based Authorization

📂 Main Application Modules
BookVerse
│
├── Authentication
│   ├── Registration
│   ├── Login
│   └── JWT Authentication
│
├── Book Catalogue
│   ├── Book Listing
│   ├── Search
│   ├── Category Filtering
│   └── Book Details
│
├── Shopping Cart
│   ├── Add to Cart
│   ├── Update Cart
│   └── Remove from Cart
│
└── Admin
    ├── Add Books
    ├── Update Books
    ├── Delete Books
    ├── Image Upload
    └── Catalogue Management

🗄️ Database
BookVerse uses MySQL as its relational database and Spring Data JPA for persistence.
The backend follows a layered approach for handling:
- Users
- Books
- Categories
- Cart data
- Authentication and authorization

🔌 REST API
The Spring Boot backend exposes RESTful APIs for application operations such as:
- User authentication
- User management
- Book management
- Book search
- Category filtering
- Cart operations
- Admin operations
The APIs were tested and debugged using Postman.

🔒 Security
BookVerse implements:
- JWT-based authentication
- Role-based authorization
- Protected REST endpoints
- Authentication-aware API requests
- Admin-specific access control

🧪 API Testing
Postman was used during development to test and debug REST APIs.
Testing included:
- Authentication requests
- Protected endpoints
- Book CRUD operations
- Cart operations
- Admin APIs

👨‍💻 Author
Saurabh Kumbhar
Computer Science & Engineering Student
GitHub:
https://github.com/2807saurabh
LinkedIn:
https://www.linkedin.com/in/saurabh-kumbhar-73a9042a1
