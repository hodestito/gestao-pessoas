CREATE TABLE employees (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    department_id UUID NOT NULL,
    position_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(16) NOT NULL,
    CONSTRAINT uk_employees_email UNIQUE (email),
    CONSTRAINT ck_employees_email_normalized CHECK (email = lower(trim(email))),
    CONSTRAINT fk_employees_department FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT fk_employees_position FOREIGN KEY (position_id) REFERENCES positions(id),
    CONSTRAINT ck_employees_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
