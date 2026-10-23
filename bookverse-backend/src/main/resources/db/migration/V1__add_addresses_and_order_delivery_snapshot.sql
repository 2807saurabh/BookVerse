CREATE TABLE addresses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    recipient_name VARCHAR(120) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    address_line VARCHAR(180) NOT NULL,
    locality VARCHAR(120) NOT NULL,
    landmark VARCHAR(120) NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    pin_code VARCHAR(10) NOT NULL,
    country VARCHAR(80) NOT NULL DEFAULT 'India',
    default_address BIT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_addresses_user (user_id),
    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users (id)
);

ALTER TABLE orders
    ADD COLUMN delivery_recipient_name VARCHAR(120) NULL,
    ADD COLUMN delivery_phone_number VARCHAR(20) NULL,
    ADD COLUMN delivery_address_line VARCHAR(180) NULL,
    ADD COLUMN delivery_locality VARCHAR(120) NULL,
    ADD COLUMN delivery_landmark VARCHAR(120) NULL,
    ADD COLUMN delivery_city VARCHAR(100) NULL,
    ADD COLUMN delivery_state VARCHAR(100) NULL,
    ADD COLUMN delivery_pin_code VARCHAR(10) NULL,
    ADD COLUMN delivery_country VARCHAR(80) NULL;
