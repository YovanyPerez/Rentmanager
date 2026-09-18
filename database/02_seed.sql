-- Demo data for local development (no users: the admin account is created in Phase 5 with a BCrypt hash).

SET NAMES utf8mb4;

INSERT INTO owners (id, full_name, email, phone)
VALUES (1, 'Ana Propietaria', 'ana@example.com', '+57 300 111 2222');

INSERT INTO tenants (id, full_name, email, phone)
VALUES (1, 'Tomás Inquilino', 'tomas@example.com', '+57 300 333 4444');

INSERT INTO properties (id, owner_id, address, city, monthly_rent, currency, status)
VALUES (1, 1, 'Carrera 7 # 45-12, Apto 302', 'Bogotá', 1800000.00, 'COP', 'AVAILABLE'),
       (2, 1, 'Calle 10 # 40-20, Apto 501', 'Medellín', 1500000.00, 'COP', 'MAINTENANCE');
