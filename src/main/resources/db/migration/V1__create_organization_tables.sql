CREATE TABLE departments (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    cost_center_code VARCHAR(50) NOT NULL
);

CREATE TABLE positions (
    id UUID PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    level VARCHAR(50) NOT NULL
);
