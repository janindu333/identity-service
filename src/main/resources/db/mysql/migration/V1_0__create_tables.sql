
CREATE TABLE IF NOT EXISTS t_user_credentials (
                                                  id INT AUTO_INCREMENT PRIMARY KEY,
                                                  name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role_id BIGINT,
    phone_number VARCHAR(20),
    latitude VARCHAR(20),
    longitude VARCHAR(20)
    );

CREATE TABLE IF NOT EXISTS t_roles (
                                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       name VARCHAR(255) NOT NULL,
    description VARCHAR(255)
    );

INSERT INTO t_roles (name, description) VALUES ('ADMIN', 'Administrator role');
