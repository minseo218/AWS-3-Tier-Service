-- 데이터베이스 선택
USE loan;

-- 테이블 생성
CREATE TABLE user (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    loan_amount DECIMAL(10, 2) NOT NULL,
    loan_duration INT NOT NULL
);

CREATE TABLE company (
    id INT AUTO_INCREMENT PRIMARY KEY,
    company_name VARCHAR(255) NOT NULL,
    interest_rate DECIMAL(5, 4) NOT NULL
);

-- 초기 데이터 삽입
INSERT INTO user (name, loan_amount, loan_duration) VALUES ('John Doe', 10000.00, 12);
INSERT INTO company (company_name, interest_rate) VALUES ('Company A', 0.0500);
INSERT INTO company (company_name, interest_rate) VALUES ('Company B', 0.0700);
INSERT INTO company (company_name, interest_rate) VALUES ('Company C', 0.0300);

