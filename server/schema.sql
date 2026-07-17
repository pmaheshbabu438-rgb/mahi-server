-- ==========================================
-- SignalConnect PostgreSQL Database Schema
-- ==========================================

-- Create extension for UUID generation if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Table 1: Users
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(100) PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    email VARCHAR(150) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    is_admin BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Table 2: Certified Routers
CREATE TABLE IF NOT EXISTS routers (
    bssid VARCHAR(17) PRIMARY KEY, -- MAC Address format, e.g. "00:1A:2B:3C:4D:5E"
    ssid VARCHAR(100) NOT NULL,
    signal_strength_dbm INT DEFAULT -70,
    download_speed_mbps REAL NOT NULL,
    upload_speed_mbps REAL NOT NULL,
    latency_ms INT DEFAULT 20,
    distance_meters REAL DEFAULT 10.0,
    security_type VARCHAR(50) DEFAULT 'WPA3 Secure',
    is_registered BOOLEAN DEFAULT TRUE,
    capacity_max INT DEFAULT 100,
    capacity_active INT DEFAULT 0,
    billing_rate VARCHAR(50) DEFAULT 'Free',
    description VARCHAR(255) DEFAULT '',
    owner_id VARCHAR(100) REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Table 3: Session Connection Logs (for usage stats caching)
CREATE TABLE IF NOT EXISTS usage_logs (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(100) REFERENCES users(id) ON DELETE CASCADE,
    router_bssid VARCHAR(17) REFERENCES routers(bssid) ON DELETE CASCADE,
    duration_seconds INT NOT NULL,
    megabytes_consumed REAL NOT NULL,
    avg_download_speed REAL NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for lightning fast lookups
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_routers_registered ON routers(is_registered);
CREATE INDEX IF NOT EXISTS idx_usage_logs_user ON usage_logs(user_id);

-- Insert starting default community router nodes
INSERT INTO routers (bssid, ssid, signal_strength_dbm, download_speed_mbps, upload_speed_mbps, latency_ms, distance_meters, security_type, billing_rate, description) 
VALUES 
('00:1A:2B:3C:4D:5E', 'SignalConnect_Alpha-7', -42, 85.5, 35.2, 12, 3.2, 'WPA3 Secure', 'Free Community Link', 'Ultra-speed node deployed in Central Transit Hub.')
ON CONFLICT (bssid) DO NOTHING;

INSERT INTO routers (bssid, ssid, signal_strength_dbm, download_speed_mbps, upload_speed_mbps, latency_ms, distance_meters, security_type, billing_rate, description) 
VALUES 
('24:F5:A2:8B:10:9C', 'Downtown_Greenway_Node', -65, 38.0, 12.5, 28, 14.8, 'WPA2 Enterprise', 'Free Public', 'Outdoor greenway coverage zone.')
ON CONFLICT (bssid) DO NOTHING;

INSERT INTO routers (bssid, ssid, signal_strength_dbm, download_speed_mbps, upload_speed_mbps, latency_ms, distance_meters, security_type, billing_rate, description) 
VALUES 
('E2:81:B4:9C:FD:0F', 'RescueNet_Gateway_Base', -81, 15.2, 4.0, 45, 24.5, 'Open Gateway', 'Free Emergency', 'Weak signal cellular extender provided during local outages.')
ON CONFLICT (bssid) DO NOTHING;
