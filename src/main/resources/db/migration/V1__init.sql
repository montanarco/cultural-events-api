CREATE TABLE cultural_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    type VARCHAR(50),
    date DATE,
    location VARCHAR(255),
    address VARCHAR(255),
    description TEXT
);