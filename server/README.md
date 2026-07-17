# SignalConnect — Full-Stack Backend Service

SignalConnect is a robust community-driven utility built to resolve weak coverage issues by linking client terminals directly to certified, high-performance community router gateways and emergency extenders. 

This repository contains the **production-ready Rest API Backend** built with **Node.js, Express, and PostgreSQL**. 

---

## Technical Architecture

- **Runtime Engine**: Node.js (Express framework)
- **Primary Database**: PostgreSQL (for users, router node registries, and statistics)
- **Security Auditing**: BCryptJS for server-side salted cryptographic hashing of access keys.
- **Session Transport**: Stateless JSON Web Tokens (JWT) signed using cryptographic secrets, exchanged via `Authorization: Bearer <TOKEN>` HTTP headers.
- **Fail-Safe Mechanism**: Automatic memory-backed sandbox container fallback if PostgreSQL instances are offline or configurations are empty.

---

## 📂 Project Directory Map
```
/server
  ├── package.json          # Dependency and runtime metadata
  ├── server.js             # Core Express server engine
  ├── schema.sql            # PostgreSQL database creation script
  └── README.md             # Setup guide and API documentation
```

---

## ⚙️ Environment Configuration

Create a file named `.env` in the `/server` folder:

```properties
PORT=3000
JWT_SECRET=your_jwt_signing_token_secret_here

# PostgreSQL database connection configs
DB_HOST=localhost
DB_PORT=5432
DB_USER=postgres
DB_PASSWORD=your_db_password_here
DB_NAME=signalconnect

# OR use direct Connection String:
# DATABASE_URL=postgresql://postgres:password@localhost:5432/signalconnect
```

---

## 🚀 Quick Setup Instructions

### 1. Database Provisioning
Run the SQL queries inside `/server/schema.sql` on your PostgreSQL database to construct tables and load initial community router gateways:
```bash
psql -U postgres -d signalconnect -f schema.sql
```

### 2. Dependency Installation
Navigate to the `/server` folder and install packages:
```bash
cd server
npm install
```

### 3. Start Development Server
```bash
npm run dev
```
The server will start listening at `http://localhost:3000`.

---

## 📋 REST API Reference

All requests must set the header `Content-Type: application/json`.

### 1. Authentication Endpoints

#### `POST /api/auth/register`
Create a new client or administrator node.
- **Request Body**:
  ```json
  {
    "email": "user@signalconnect.com",
    "name": "Jane User",
    "password": "userpassword123",
    "isAdmin": false
  }
  ```
- **Success Response (201 Created)**:
  ```json
  {
    "success": true,
    "message": "Account registered successfully.",
    "data": {
      "id": "user_1714241",
      "email": "user@signalconnect.com",
      "name": "Jane User",
      "token": "eyJhbGciOiJIUzI1NiIsIn...",
      "isAdmin": false
    }
  }
  ```

#### `POST /api/auth/login`
Aquire session token.
- **Request Body**:
  ```json
  {
    "email": "user@signalconnect.com",
    "password": "userpassword123"
  }
  ```
- **Success Response (200 OK)**:
  ```json
  {
    "success": true,
    "message": "Handshake successful.",
    "data": {
      "id": "user_1714241",
      "email": "user@signalconnect.com",
      "name": "Jane User",
      "token": "eyJhbGciOiJIUzI1NiIsIn...",
      "isAdmin": false
    }
  }
  ```

---

### 2. Certified Router Gateway Endpoints

*These endpoints require authenticating with a JWT token inside the **Authorization** header:*
`Authorization: Bearer <TOKEN>`

#### `GET /api/routers`
Fetch complete list of currently certified community router gateway nodes available.
- **Success Response (200 OK)**:
  ```json
  {
    "success": true,
    "data": [
      {
        "bssid": "00:1A:2B:3C:4D:5E",
        "ssid": "SignalConnect_Alpha-7",
        "signalStrengthDbm": -42,
        "downloadSpeedMbps": 85.5,
        "uploadSpeedMbps": 35.2,
        "latencyMs": 12,
        "distanceMeters": 3.2,
        "securityType": "WPA3 Secure",
        "isRegistered": true,
        "capacityMax": 150,
        "capacityActive": 18,
        "billingRate": "Free Community Link",
        "description": "Ultra-speed node deployed in Central Transit Hub."
      }
    ]
  }
  ```

#### `POST /api/routers` (Admin-Only)
Register and certify a brand new community access gateway node.
- **Request Body**:
  ```json
  {
    "bssid": "00:E0:4C:68:01:AA",
    "ssid": "SignalConnect_Beta-12",
    "downloadSpeedMbps": 60.5,
    "uploadSpeedMbps": 25.0,
    "securityType": "WPA3 Secure",
    "billingRate": "Free Community",
    "description": "Backup power node positioned at the Community Shelter."
  }
  ```
- **Success Response (210 Created)**:
  ```json
  {
    "success": true,
    "message": "Router node certified successfully.",
    "data": {
      "bssid": "00:E0:4C:68:01:AA",
      "ssid": "SignalConnect_Beta-12",
      "signalStrengthDbm": -55,
      "downloadSpeedMbps": 60.5,
      "uploadSpeedMbps": 25.0,
      "latencyMs": 18,
      "distanceMeters": 6.5,
      "securityType": "WPA3 Secure",
      "isRegistered": true,
      "capacityMax": 100,
      "capacityActive": 0,
      "billingRate": "Free Community",
      "description": "Backup power node positioned at the Community Shelter."
    }
  }
  ```

#### `DELETE /api/routers/:bssid` (Admin-Only)
De-certify and remove a router gateway hardware node registry.
- **Success Response (200 OK)**:
  ```json
  {
    "success": true,
    "message": "Router node de-certified successfully."
  }
  ```
