# Procurement System - React + Spring Boot Integrated

## Backend
1. Open `backend` in IntelliJ/Eclipse.
2. Ensure MySQL database `procurement_db` is running and matches `backend/src/main/resources/application.yml`.
3. Run the Spring Boot application.
4. Backend URL: `http://localhost:8081`

## Frontend
1. Open a terminal in `frontend`.
2. Run `npm install`.
3. Run `npm run dev`.
4. Open `http://localhost:5173`.

The React app now uses Axios and JWT from the backend. The API URL defaults to `http://localhost:8081/api` and can be changed with `VITE_API_URL`.

## Connected workflow
- Register/login -> JWT authentication
- User creates and submits purchase request
- Admin sees pending requests and approves/rejects
- Supplier console sends payment details
- User marks payment as paid
- Supplier console sends shipment/tracking updates
- User sees tracking history
- User submits supplier rating
- Admin sees users/products/suppliers/ratings
- User can download their payment CSV

Supplier registration/login is intentionally not added because the backend exposes supplier actions as public supplier endpoints.

## Product images
The frontend includes five preloaded product photos in `frontend/public/product-images/`:
- A4 sheets
- Chair
- Laptop
- Printer
- Table

The Product Board automatically uses these images when a backend product name/description/SKU contains the matching product keyword. If `imageData` is present on a product, the backend image still takes priority.
