package com.leets.blog.domain.report.entity;

public enum ReportReason {
    SPAM, // 스팸, 홍보글
    ABUSE, // 욕설, 비방
    ADULT, // 선정성
    ETC // 기타 -> 내용은 detail에
}
