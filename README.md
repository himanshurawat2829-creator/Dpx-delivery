# DPX Courier Delivery Management System

Web-based courier delivery system with 5 entities: Customer, Courier, Parcel, Delivery, StatusHistory.
Plain Java 17+ (no external libraries needed to run). Storage is in memory.

## Run
```
javac -d out $(find src/main -name "*.java")
java -cp out:src/main/resources dpx.Main
```
Open http://localhost:8080 (on Windows use `;` instead of `:` in -cp).
In IntelliJ: open the folder, mark src/main/java as Sources, src/main/resources as Resources, run `dpx.Main`.

## Tests (JUnit 4)
Add JUnit 4.13.2 and hamcrest-core 1.3 to the classpath, then run `dpx.DpxTest`.
In IntelliJ: right-click `src/test/java/dpx/DpxTest.java` > Run (add JUnit 4 library when asked).

## REST API
/api/customers, /api/couriers, /api/packages (parcels), /api/deliveries: GET, POST, PUT, DELETE.
/api/history: GET only. /api/stats: GET.
