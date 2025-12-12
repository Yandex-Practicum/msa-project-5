CREATE TABLE clients (
    client_id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100),
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE orders (
    order_id SERIAL PRIMARY KEY,
    client_id INT REFERENCES clients(client_id),
    amount DECIMAL(10, 2),
    order_date DATE
);


INSERT INTO clients (name, email) VALUES 
('Alice Wang', 'alice_wang@gmail.com'),
('John Moys', 'john.moys@yahoo.com'),
('Tim Cook', 'tim.cook@apple.com');

INSERT INTO orders (order_id, client_id, amount, order_date) VALUES 
(101, 1, 150.00, '2023-11-01'), 
(102, 2, 50.00, '2023-11-02'), 
(103, 1, 150.00, '2023-11-03'), 
(104, 3, 120.00, '2023-11-01'), 
(105, 2, 50.00, '2023-11-04');
