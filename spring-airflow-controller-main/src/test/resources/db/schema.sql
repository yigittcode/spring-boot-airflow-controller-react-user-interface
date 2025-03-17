-- Simplified schema for testing
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(255) PRIMARY KEY,
    created_timestamp BIGINT,
    username VARCHAR(255) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    totp BOOLEAN DEFAULT FALSE, 
    email_verified BOOLEAN DEFAULT FALSE,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    email VARCHAR(255),
    CONSTRAINT uk_users_username UNIQUE (username)
);

-- İndeksleri oluştur
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Audit logs tablosunu oluştur
CREATE TABLE IF NOT EXISTS audit_logs (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL,
    dag_id VARCHAR(255) NOT NULL,
    dag_run_id VARCHAR(255),
    operation VARCHAR(50) NOT NULL,
    operation_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    details TEXT,
    CONSTRAINT fk_audit_logs_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Audit logs indekslerini oluştur
CREATE INDEX IF NOT EXISTS idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_dag_id ON audit_logs(dag_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_operation ON audit_logs(operation);
CREATE INDEX IF NOT EXISTS idx_audit_logs_operation_time ON audit_logs(operation_time); 