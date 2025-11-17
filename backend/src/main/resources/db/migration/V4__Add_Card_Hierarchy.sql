-- Spec § 6. 백엔드 규격 - 데이터베이스 스키마 확장
-- FR-06a: 부모-자식 관계 데이터 모델
-- 1단계 계층 구조 지원 (부모 → 자식, 손자 불가)

-- parent_card_id 컬럼 추가
ALTER TABLE card
ADD COLUMN parent_card_id BIGINT;

-- Self-Referential Foreign Key 설정
-- ON DELETE SET NULL: 부모 삭제 시 자식의 parent_card_id를 NULL로 설정 (실제로는 서비스 레벨에서 차단)
ALTER TABLE card
ADD CONSTRAINT fk_card_parent
FOREIGN KEY (parent_card_id) REFERENCES card(id)
ON DELETE SET NULL;

-- 성능 최적화를 위한 인덱스 생성
-- 특정 부모의 자식 카드 조회 시 사용
CREATE INDEX idx_card_parent ON card(parent_card_id);

-- 컬럼 설명
COMMENT ON COLUMN card.parent_card_id IS '부모 카드 ID (Self-Referential, nullable)';
