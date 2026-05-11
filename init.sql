-- Native PostgreSQL ENUM types for all services
-- This file runs automatically when the postgres container is first created
-- Docker mounts this file into /docker-entrypoint-initdb.d/

-- user-service enums
CREATE TYPE user_role AS ENUM ('CUSTOMER', 'ADMIN');
CREATE TYPE user_status AS ENUM ('ACTIVE', 'DEACTIVATED');

-- product-service enums (category is VARCHAR, not enum, to support case-insensitive values)
CREATE TYPE product_status AS ENUM ('ACTIVE', 'INACTIVE', 'DISCONTINUED');

-- order-service enums
CREATE TYPE order_status AS ENUM (
    'PENDING', 'CONFIRMED', 'SHIPPED',
    'DELIVERED', 'CANCELLED', 'RETURNED'
);

-- shipping-service enums
CREATE TYPE shipment_status AS ENUM (
    'PROCESSING', 'SHIPPED', 'IN_TRANSIT',
    'OUT_FOR_DELIVERY', 'DELIVERED', 'RETURNED'
);

-- billing-service enums
CREATE TYPE transaction_method AS ENUM (
    'CREDIT_CARD', 'CASH_ON_DELIVERY', 'WALLET'
);
CREATE TYPE transaction_status AS ENUM (
    'PENDING', 'COMPLETED', 'FAILED', 'REFUNDED'
);
CREATE TYPE discount_type AS ENUM ('PERCENTAGE', 'FIXED');


