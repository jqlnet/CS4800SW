# Receipt Tracker

Software Development project for CS 4800 Team 3

A web application that helps everyday shoppers stay on top of their purchases. Log receipts, track 30-day refund windows automatically, and monitor EBT spending — all in one place.

---

## Tech Stack

- **Java 21**
- **Spring Boot 4.0.3**
- **Thymeleaf** — server-side HTML templating
- **Maven** — dependency management
- **HTML / CSS / JavaScript** — frontend UI with dark and light mode

---

## Prerequisites

Before running the project, make sure you have the following installed:

- [Java 21](https://adoptium.net/)
- [Maven](https://maven.apache.org/) (or use the included `mvnw` wrapper)
- A Java IDE or editor — [VS Code](https://code.visualstudio.com/) with the Spring Boot Extension Pack recommended

---

## How to Run Locally

1. **Clone the repository**
   ```bash
   git clone https://github.com//CS4800SW.git
   cd ReceiptTracker
   ```

2. **Run the application**

   Using the Maven wrapper (no Maven install needed):
   ```bash
   # macOS / Linux
   ./mvnw spring-boot:run

   # Windows
   mvnw.cmd spring-boot:run
   ```

   Or if Maven is installed globally:
   ```bash
   mvn spring-boot:run
   ```

3. **Open in your browser**
   ```
   http://localhost:8080
   ```

---

## Features

- Add receipts manually with vendor, date, amount, and payment type (Card, Cash, EBT)
- Automatic 30-day refund deadline calculation from purchase date
- Refund status tracking — see which receipts are still **Refundable** or **Expired**
- EBT spend tracking with a live running total on the dashboard
- Search receipts by vendor or payment type
- Filter receipts by payment type or refund status
- Delete receipts
- Summary dashboard showing total receipts, total spent, EBT spent, and refundable count
- Dark and light mode toggle that persists across pages

---

## Project Structure

```
src/
└── main/
    ├── java/edu/cpp/cs4800/receipttracker/
    │   ├── ReceiptTrackerApplication.java   # App entry point
    │   ├── ReceiptController.java           # HTTP routes and request handling
    │   └── model/
    │       └── Receipt.java                 # Receipt entity and business logic
    └── resources/
        └── templates/
            ├── home.html                    # Landing page
            ├── receipts.html                # Receipt dashboard and table
            └── add-receipt.html             # Add receipt form
```

---

## To Be Implemented

- [ ] User login and registration
- [ ] Database persistence (currently resets on app restart)
- [ ] Receipt photo upload with OCR text extraction
- [ ] Import receipts from online order URLs
- [ ] Email notifications for upcoming refund deadlines

---

## Contributors

- Daniel Chahbour
- Manuel Abanto
- Toni Liang
