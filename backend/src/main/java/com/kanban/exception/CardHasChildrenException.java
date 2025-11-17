package com.kanban.exception;

import lombok.Getter;

/**
 * 자식 카드가 있는 부모 카드 삭제 시도 시 발생하는 예외
 * Spec § 7. 보안 처리 - 데이터 무결성
 * FR-06h 변경: 부모 카드 삭제 차단 (자식 존재 시)
 * 결정 사항 2: 자식이 있으면 부모 삭제 차단
 */
@Getter
public class CardHasChildrenException extends RuntimeException {

    private final int childCount;

    public CardHasChildrenException(int childCount) {
        super("자식 카드가 있는 부모 카드는 삭제할 수 없습니다. 먼저 모든 자식 카드를 삭제해주세요.");
        this.childCount = childCount;
    }

    public CardHasChildrenException(String message, int childCount) {
        super(message);
        this.childCount = childCount;
    }
}
