

# 📰 News Reader – Java Swing OOP Project

## 📖 Overview

This project is a desktop-based **News Reader application** developed as part of my **CSE215: Object-Oriented Programming (Java)** course.
It demonstrates the practical implementation of **OOP concepts**, **GUI development**, and **API integration** in Java.

The application allows users to fetch and read the latest news headlines by selecting different categories through a simple and interactive interface.

---

## 🎯 Objectives

* Apply core **Object-Oriented Programming (OOP)** principles
* Build a functional **GUI application using Java Swing**
* Integrate **real-time API data** into a desktop app
* Ensure smooth user experience with **multithreading**

---

## 🚀 Features

* 📡 Fetch latest news from an online API
* 🗂️ Category-based filtering:

  * General
  * Technology
  * Sports
  * Business
  * Science
  * Health
  * Entertainment
* 🖱️ Clickable headlines (opens full article in browser)
* ⚡ Smooth UI using `SwingWorker` (no freezing)
* 📦 Offline fallback data (works without internet)
* 🎨 Clean and user-friendly interface

---

## 🧠 OOP Concepts Demonstrated

| Concept       | Implementation                                |
| ------------- | --------------------------------------------- |
| Encapsulation | `Article` class with private fields + getters |
| Abstraction   | `NewsService` interface                       |
| Polymorphism  | `GNewsService` implements `NewsService`       |
| Inheritance   | GUI components (`JFrame`, `JPanel`)           |

---

## 🛠️ Technologies Used

* **Java**
* **Java Swing (GUI Framework)**
* **HTTP Networking (API Calls)**
* **GNews API**

---

## 🏗️ Project Structure

```
NewsReader.java
 ├── Article (Model Class)
 ├── NewsService (Interface)
 ├── GNewsService (Service Implementation)
 ├── ArticleCard (UI Component)
 └── MainFrame (Main GUI)
```

---

## ▶️ How to Run

1. Clone this repository

   ```bash
   git clone https://github.com/your-username/news-reader.git
   ```
2. Open in IntelliJ IDEA or any Java IDE
3. Navigate to `NewsReader.java`
4. Run the program ▶️
5. Select a category and click **Get News**


---

## ⚠️ Notes

* The app uses a public API, which may have request limits
* If API fails or no internet connection is available, **mock data** is displayed
* Designed for learning purposes (academic project)


---

## ⭐ Acknowledgment

This project was developed as part of academic coursework to practice **Java (OOP) **.

---



