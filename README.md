# 🎵 Spotify Manager - Java OOP Final Project
![Java](https://img.shields.io/badge/Java-17-orange.svg)
![MySQL](https://img.shields.io/badge/MySQL-8.0+-blue.svg)
![Maven](https://img.shields.io/badge/Build-Maven-success.svg)

## 📌 Project Overview
**Spotify Manager** is a comprehensive desktop application built with Java Swing and JDBC. Designed as the final project for the Object-Oriented Programming course, this application allows administrators and users to efficiently manage a massive dataset of Spotify tracks, artists, albums, and genres.

### 🔑 Key Features
* **Authentication & Authorization:** Secure login/registration with SHA-256 password hashing. Role-Based Access Control (Admin vs. User).
* **Advanced CRUD Operations:** Manage tracks and artists with dynamic pagination, search, and filtering.
* **Interactive Dashboards:** Real-time Data Visualization using JFreeChart (Bar Charts & Pie Charts).
* **Multithreaded Export:** Export large datasets to Excel (`.xlsx`) via Apache POI using background `SwingWorker` threads to prevent UI freezing.
* **Modern Java Practices:** Implementation of Java 17 features including `Record` classes, `var`, Enhanced `switch` expressions, and Stream API.
* **Design Patterns:** Strict adherence to Singleton (Database Connection) and DAO architecture.
* **Creative UI/UX:** Dynamic Global Theme Switcher that recursively applies custom color palettes to all UI components.
  
---

## 🛠️ Prerequisites
Before you begin, ensure you have met the following requirements:
* **Java Development Kit (JDK):** Version 17 or higher.
* **Database:** MySQL Server (8.0+) and MySQL Workbench.
* **IDE:** Eclipse IDE for Enterprise Java (or IntelliJ IDEA) with Maven integration.

---

### 🗄️ Database Server
- **Database:** [MySQL Community Server v8.0 or higher](https://dev.mysql.com/downloads/mysql/)
- **Database GUI Client:** [MySQL Workbench](https://dev.mysql.com/downloads/workbench/)

### 🔌 Drivers & Dependencies
- **JDBC Driver:** This project uses **Apache Maven** for dependency management. Downloading the MySQL JDBC driver manually is not necessary.
  
---

## 🛠️ Setup Steps

Below are the instructions to set up the environments locally:
1. Open **MySQL Workbench** and connect to your local MySQL server.
2. Open a new SQL tab and create the database by running:
   ```sql
   CREATE DATABASE spotify_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   USE spotify_db;
   
Locate the database/ folder in this repository.

Step 1 - Create spotify_raw Table: Open and execute CreateRaw.sql

Step 2 - Import Data: Open ImportData.sql and execute it. Note: This file contains thousands of records, so it may take a few seconds to run.

Step 3 - Create Tables: Open and execute CreateTables.sql 

Verify: Run SELECT * FROM Tracks LIMIT 10; to ensure the data was imported successfully.

## 🚀 How to run the app
Step 1: Clone the repository: 
        git clone [https://github.com/your-username/your-repo-name.git](https://github.com/your-username/your-repo-name.git)

Step 2: Go to File -> Import... -> Maven -> Existing Maven Projects.
        Browse to the cloned directory and click Finish.
        
Step 3: Configure Database Credentials:
        Navigate to src/main/java/siu/k18/cntt/spotify/utils/DatabaseConnection.java.
        Update the PASSWORD variable with your local MySQL root password:
        private final String PASSWORD = "your_local_mysql_password";
        
Step 4: Install Dependencies:
        Right-click the project folder in Eclipse -> Maven -> Update Project... -> Click OK. Wait for Maven to download JFreeChart, Apache POI, and JUnit 5.

Step 5: Run the Application:
        Navigate to src/main/java/siu/k18/cntt/spotify/gui/LoginFrame.java.
        Right-click the file -> Run As -> Java Application.
        
To access the system immediately, you can register a new account on the Login screen, or use the database to manually insert an Admin account.

---

## 📸 Screenshots

* **Authentication (Login & Registration):** Secure entry point with role-based validation.
    ![Login Screen](<img width="575" height="357" alt="Screenshot 2026-05-29 213406" src="https://github.com/user-attachments/assets/e36799c3-ed96-4f6c-8b0c-129a89d7ea17" />)

* **Main Dashboard & CRUD Operations:** Complete track management with dynamic search, filtering, and pagination.
    ![Main Dashboard](<img width="1625" height="959" alt="Screenshot 2026-05-29 213904" src="https://github.com/user-attachments/assets/19409be6-5b70-4ba8-a2f3-c26eefa3a5ab" />)

* **Data Visualization:** Real-time Bar and Pie charts generated via JFreeChart reflecting top tracks and artists.
    ![Charts View](<img width="1625" height="955" alt="Screenshot 2026-05-29 213938" src="https://github.com/user-attachments/assets/5c48cd53-6e73-4775-be5c-6ac0caebb414" />)

* **Dynamic Theme Switcher:** Custom recursive UI color repainting in action.
    ![Theme Switcher](<img width="1625" height="955" alt="Screenshot 2026-05-29 214006" src="https://github.com/user-attachments/assets/8e3b86b0-34bb-4f50-be2b-f6993a2d730c" />)

---

## ⚠️ Known limitations
* **Local Database Dependency:** The application currently relies on a locally hosted MySQL server. Transitioning to a cloud-based relational database (like AWS RDS or Supabase) is required for true multi-user network access.

* **Media Playback:** As a data management tool, the application manages the metadata of Spotify tracks (duration, tempo, danceability) but does not integrate with the Spotify Web API for actual audio streaming or playback.

* **Password Recovery:** The authentication system features secure SHA-256 hashing, but automated password recovery (e.g., via Email OTP) is not yet implemented. Forgotten passwords must be manually reset by an Administrator directly in the database.

* **Theme Switcher Limitations:** While the recursive theme switcher works globally across most Swing components, certain native OS dialogue boxes (like the FileChooser for Excel export) may retain default Windows/macOS styling.

---

## 👥Authors

- Trần Quốc Thịnh: tranquocthinhk18@siu.edu.vn

- Thái Trọng An: thaitrongank18@siu.edu.vn

---

*Developed for the purpose of education.*
