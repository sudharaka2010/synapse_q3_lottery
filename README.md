# 🎟️ Synapse Q3 – Lottery Results Scraper + Database Save (Spring Boot + JPA)

![Java](https://img.shields.io/badge/Java-17-red?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?style=for-the-badge&logo=springboot)
![JPA](https://img.shields.io/badge/Hibernate%20%2F%20JPA-ORM-blue?style=for-the-badge)
![Jsoup](https://img.shields.io/badge/Jsoup-Web%20Scraping-orange?style=for-the-badge)
![Database](https://img.shields.io/badge/Database-PostgreSQL%20%2F%20MySQL-336791?style=for-the-badge&logo=postgresql)
![License](https://img.shields.io/badge/License-Educational-lightgrey?style=for-the-badge)

> **Module / Assignment:** Synapse – Q3  
> **Task:** Scrape latest lottery results and store them in a relational database using **Spring Boot + Hibernate/JPA**.

---

## 📌 Overview

This application is a **console-based Spring Boot** project that:

✅ Loads lottery results from a public lottery results website  
✅ Extracts: **Lottery Name, Draw Number, Draw Date, Winning Numbers, Source URL**  
✅ Saves results into a database table `lottery_result26` using **Hibernate/JPA**  
✅ Prevents duplicate rows using a **unique constraint** (`lottery_name + draw_no`)  
✅ Adds an automatic **created_at timestamp** when saving

---

## ✨ Features

- ✅ Console menu (choose lottery 1–7)
- ✅ Web scraping using **Jsoup**
- ✅ Data validation before saving
- ✅ Duplicate prevention (unique constraint + repo check)
- ✅ Stores historical results (does **NOT** overwrite existing rows)
- ✅ Adds `created_at` automatically using `@PrePersist`

---

## 🧠 How the Duplicate Logic Works

This project follows this logic:

### ✅ Save new record when:
- `lotteryName` exists
- draw number is **new** (not already stored)

### 🚫 Do NOT save when:
- same `lotteryName + drawNo` already exists  
  → prints a message like:  
  **"No new draw detected for this lottery today. Latest draw is already stored."**

> ✅ This keeps **full history** of results over time.

---

## 🏗️ Project Structure

synapse_q3_lottery/
├── src/main/java/com/synapse/lotterydb/
│ ├── SynapseQ3LotteryApplication.java
│ ├── LotteryResultsScraper.java
│ ├── entity/
│ │ └── LotteryResult26.java
│ └── repo/
│ └── LotteryResult26Repo.java
└── src/main/resources/
└── application.properties


---

## 🗃️ Database Table

Table name: **`lottery_result26`**

| Column | Type | Description |
|-------|------|-------------|
| `id` | INT (PK) | Auto increment |
| `lottery_name` | VARCHAR(50) | Lottery name |
| `draw_no` | INT | Draw number |
| `draw_date` | DATE | Draw date |
| `result_numbers` | VARCHAR(50) | Winning numbers |
| `source_url` | VARCHAR(255) | Source page |
| `created_at` | TIMESTAMP | System time when saved |

✅ Unique constraint: `lottery_name + draw_no`

---

## ⚙️ Setup & Run

### ✅ Requirements
- Java 17+
- Maven
- Database: PostgreSQL **or** MySQL
- Internet access (to fetch results)




