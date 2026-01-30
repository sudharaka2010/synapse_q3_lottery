# Q3 – Lottery Results Scraper + Save to MySQL (Spring Boot + JPA)

This project is my **Assignment Q3** implementation.  
It **scrapes Sri Lanka lottery results** from the GovDoc website and **stores the results in a MySQL database** using **Spring Boot + Hibernate (Spring Data JPA)**.

---

## ✅ What Q3 Does

- Connects to GovDoc lottery result pages (using **Jsoup**)
- Extracts:
  - Lottery name
  - Draw number
  - Draw date
  - Winning numbers
  - Source URL (page link)
- Saves data into MySQL table: **`a1.lottery_result26`**
- Prevents duplicates (same **lottery_name + draw_no** will not be inserted twice)

---

## 🛠 Tech Stack

- Java 17
- Spring Boot
- Spring Data JPA (Hibernate)
- MySQL
- Jsoup
- Maven

---

## 📂 Project Structure (Main Files)

