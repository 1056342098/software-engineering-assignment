CREATE TABLE process_timeline_node (
  id BIGINT NOT NULL AUTO_INCREMENT,
  approval_type VARCHAR(32) NOT NULL,
  stage_index INT NOT NULL,
  stage_code VARCHAR(32) NOT NULL,
  stage_name VARCHAR(64) NOT NULL,
  interval_days INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_process_timeline_node (approval_type, stage_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO process_timeline_node (approval_type, stage_index, stage_code, stage_name, interval_days) VALUES
('PARTY', 0, 'APPLICANT', '入党申请人', 0),
('PARTY', 1, 'ACTIVE', '积极分子', 180),
('PARTY', 2, 'DEVELOPMENT', '发展对象', 365),
('PARTY', 3, 'PROBATIONARY', '预备党员', 30),
('PARTY', 4, 'FULL', '正式党员', 365);

INSERT INTO process_timeline_node (approval_type, stage_index, stage_code, stage_name, interval_days) VALUES
('LEAGUE', 0, 'APPLICANT', '入团申请', 0),
('LEAGUE', 1, 'PROBATIONARY', '预备团员', 90),
('LEAGUE', 2, 'FULL', '正式团员', 180);

CREATE TABLE qa_question (
  id BIGINT NOT NULL AUTO_INCREMENT,
  type VARCHAR(32) NOT NULL,
  content TEXT NOT NULL,
  options_json TEXT NOT NULL,
  correct_answer VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO qa_question (type, content, options_json, correct_answer) VALUES
('PARTY', '中国共产党成立于哪一年？', '["1919年", "1921年", "1949年", "1978年"]', '1921年'),
('PARTY', '中国共产党的根本宗旨是？', '["全心全意为人民服务", "实现共产主义", "建设中国特色社会主义", "全面建成小康社会"]', '全心全意为人民服务'),
('LEAGUE', '中国共产主义青年团是中国共产党的？', '["助手和后备军", "先锋队", "核心力量", "领导核心"]', '助手和后备军');
