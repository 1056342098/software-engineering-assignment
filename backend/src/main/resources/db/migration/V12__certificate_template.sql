CREATE TABLE certificate_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    content TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO certificate_template (name, content) VALUES
('在读证明', '兹证明 ${realName}（学号：${studentNo}），系我校 ${major} 专业 ${grade} 级 ${className} 班全日制在读本科生。\n\n特此证明。');
