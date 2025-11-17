-- OAuth 2.0 소셜 로그인 지원을 위한 스키마 변경

-- 1. User 테이블 수정: password를 nullable로, primary_provider 추가
ALTER TABLE users ALTER COLUMN password VARCHAR(255) NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS primary_provider VARCHAR(20) DEFAULT 'LOCAL' NOT NULL;

-- 2. UserIdentity 테이블 생성
CREATE TABLE user_identities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    email VARCHAR(150),
    linked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_identities_user_id FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT unique_user_provider UNIQUE (user_id, provider),
    CONSTRAINT unique_provider_id UNIQUE (provider, provider_id)
);

-- 3. 인덱스 생성
CREATE INDEX idx_user_identities_user_id ON user_identities(user_id);
CREATE INDEX idx_user_identities_provider_id ON user_identities(provider, provider_id);

-- 4. 기존 사용자에 대한 LOCAL Identity 생성
INSERT INTO user_identities (user_id, provider, provider_id, email, linked_at)
SELECT id, 'LOCAL', email, email, created_at
FROM users
WHERE NOT EXISTS (
    SELECT 1 FROM user_identities ui
    WHERE ui.user_id = users.id AND ui.provider = 'LOCAL'
);
