-- RentManager schema — source of truth (applied by docker-entrypoint-initdb.d on first container start).
-- Business state values are language-neutral codes; labels are translated in the frontend.
-- Note: status/role columns use VARCHAR + CHECK (not MySQL ENUM) for Hibernate `validate` compatibility.

CREATE TABLE users (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  email         VARCHAR(255) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  full_name     VARCHAR(150) NOT NULL,
  role          VARCHAR(10)  NOT NULL,
  active        BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uq_users_email (email),
  CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'OWNER', 'TENANT'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE owners (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  user_id    BIGINT       NULL,
  full_name  VARCHAR(150) NOT NULL,
  email      VARCHAR(255) NULL,
  phone      VARCHAR(50)  NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uq_owners_user (user_id),
  CONSTRAINT fk_owners_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE tenants (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  user_id    BIGINT       NULL,
  full_name  VARCHAR(150) NOT NULL,
  email      VARCHAR(255) NULL,
  phone      VARCHAR(50)  NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uq_tenants_user (user_id),
  CONSTRAINT fk_tenants_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE properties (
  id           BIGINT         NOT NULL AUTO_INCREMENT,
  owner_id     BIGINT         NOT NULL,
  address      VARCHAR(255)   NOT NULL,
  city         VARCHAR(100)   NOT NULL,
  description  VARCHAR(2000)  NULL,
  image_url    VARCHAR(500)   NULL,
  monthly_rent DECIMAL(10, 2) NOT NULL,
  status       VARCHAR(20)    NOT NULL DEFAULT 'AVAILABLE',
  created_at   TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at   TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_properties_owner (owner_id),
  KEY idx_properties_status (status),
  CONSTRAINT fk_properties_owner FOREIGN KEY (owner_id) REFERENCES owners (id),
  CONSTRAINT chk_properties_rent_positive CHECK (monthly_rent > 0),
  CONSTRAINT chk_properties_status CHECK (status IN ('AVAILABLE', 'RENTED', 'MAINTENANCE', 'INACTIVE'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE contracts (
  id                 BIGINT         NOT NULL AUTO_INCREMENT,
  property_id        BIGINT         NOT NULL,
  tenant_id          BIGINT         NOT NULL,
  start_date         DATE           NOT NULL,
  end_date           DATE           NOT NULL,
  monthly_rent       DECIMAL(10, 2) NOT NULL,
  status             VARCHAR(15)    NOT NULL DEFAULT 'DRAFT',
  -- Enforces: at most one ACTIVE contract per property (NULLs are not indexed as duplicates by MySQL).
  active_property_id BIGINT GENERATED ALWAYS AS (IF(status = 'ACTIVE', property_id, NULL)) STORED,
  created_at         TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at         TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uq_contracts_active_property (active_property_id),
  KEY idx_contracts_property (property_id),
  KEY idx_contracts_tenant (tenant_id),
  KEY idx_contracts_status (status),
  CONSTRAINT fk_contracts_property FOREIGN KEY (property_id) REFERENCES properties (id),
  CONSTRAINT fk_contracts_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
  CONSTRAINT chk_contracts_dates CHECK (start_date < end_date),
  CONSTRAINT chk_contracts_rent_positive CHECK (monthly_rent > 0),
  CONSTRAINT chk_contracts_status CHECK (status IN ('DRAFT', 'ACTIVE', 'EXPIRED', 'TERMINATED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE payments (
  id          BIGINT         NOT NULL AUTO_INCREMENT,
  contract_id BIGINT         NOT NULL,
  amount      DECIMAL(10, 2) NOT NULL,
  due_date    DATE           NOT NULL,
  paid_date   DATE           NULL,
  status      VARCHAR(10)    NOT NULL DEFAULT 'PENDING',
  created_at  TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at  TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uq_payments_contract_due (contract_id, due_date),
  KEY idx_payments_contract (contract_id),
  KEY idx_payments_status (status),
  CONSTRAINT fk_payments_contract FOREIGN KEY (contract_id) REFERENCES contracts (id),
  CONSTRAINT chk_payments_amount_positive CHECK (amount > 0),
  CONSTRAINT chk_payments_status CHECK (status IN ('PENDING', 'PAID', 'OVERDUE', 'CANCELLED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE maintenance_requests (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  property_id BIGINT       NOT NULL,
  created_by  BIGINT       NOT NULL,
  title       VARCHAR(150) NOT NULL,
  description VARCHAR(2000) NULL,
  status      VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
  assigned_to VARCHAR(150) NULL,
  created_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_maintenance_property (property_id),
  KEY idx_maintenance_status (status),
  CONSTRAINT fk_maintenance_property FOREIGN KEY (property_id) REFERENCES properties (id),
  CONSTRAINT fk_maintenance_created_by FOREIGN KEY (created_by) REFERENCES users (id),
  CONSTRAINT chk_maintenance_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
