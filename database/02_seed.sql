-- Demo data for local development (no users: the admin account is created in Phase 5 with a BCrypt hash).

SET NAMES utf8mb4;

INSERT INTO owners (id, full_name, email, phone)
VALUES (1, 'Ana Propietaria', 'ana@example.com', '+34 600 111 222');

INSERT INTO tenants (id, full_name, email, phone)
VALUES (1, 'Tomás Inquilino', 'tomas@example.com', '+34 600 333 444');

INSERT INTO properties (id, owner_id, address, city, monthly_rent, status)
VALUES (1, 1, 'Calle Mayor 12, 3ºA', 'Madrid', 1200.00, 'AVAILABLE'),
       (2, 1, 'Avenida del Puerto 8', 'Valencia', 950.00, 'MAINTENANCE');
