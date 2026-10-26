package com.leets.blog.domain.report.service;

import com.leets.blog.common.exception.CustomException;
import com.leets.blog.common.exception.ErrorCode;
import com.leets.blog.domain.comment.entity.Comment;
import com.leets.blog.domain.comment.service.CommentReader;
import com.leets.blog.domain.member.entity.Member;
import com.leets.blog.domain.member.service.MemberReader;
import com.leets.blog.domain.post.entity.Post;
import com.leets.blog.domain.post.service.PostReader;
import com.leets.blog.domain.report.entity.Report;
import com.leets.blog.domain.report.entity.ReportReason;
import com.leets.blog.domain.report.repository.ReportRepository;
import com.leets.blog.domain.report.web.dto.request.ReportCreateRequest;
import com.leets.blog.domain.report.web.dto.response.ReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final MemberReader memberReader;
    private final PostReader postReader;
    private final CommentReader commentReader;

    // 게시글 신고: 회원(401) → 사유(400) → 게시글 ID(400) → 게시글 존재(404) → 본인 글(400) → 중복(409)
    @Transactional
    public ReportResponse reportPost(Long memberId, Long postId, ReportCreateRequest request) {
        Member reporter = memberReader.getActiveMember(memberId);
        ReportReason reason = ReportReason.from(request.reason());
        Post post = postReader.getVisiblePost(postId);
        if (post.isWrittenBy(memberId)) {
            throw new CustomException(ErrorCode.CANNOT_REPORT_OWN_CONTENT);
        }
        if (reportRepository.existsByReporterIdAndPostId(memberId, postId)) {
            throw new CustomException(ErrorCode.DUPLICATE_REPORT);
        }
        return ReportResponse.from(save(Report.ofPost(reporter, post, reason, request.detail())));
    }

    // 댓글 신고: 회원(401) → 사유(400) → 댓글 ID(400) → 댓글 존재(404) → 본인 댓글(400) → 중복(409)
    @Transactional
    public ReportResponse reportComment(Long memberId, Long commentId, ReportCreateRequest request) {
        Member reporter = memberReader.getActiveMember(memberId);
        ReportReason reason = ReportReason.from(request.reason());
        Comment comment = commentReader.getVisibleComment(commentId);
        if (comment.isWrittenBy(memberId)) {
            throw new CustomException(ErrorCode.CANNOT_REPORT_OWN_CONTENT);
        }
        if (reportRepository.existsByReporterIdAndCommentId(memberId, commentId)) {
            throw new CustomException(ErrorCode.DUPLICATE_REPORT);
        }
        return ReportResponse.from(save(Report.ofComment(reporter, comment, reason, request.detail())));
    }

    private Report save(Report report) {
        try {
            return reportRepository.save(report);
        } catch (DataIntegrityViolationException e) {
            // 같은 대상을 동시에 신고해 중복 확인을 함께 통과한 경우 (DB unique 제약에서 걸림)
            throw new CustomException(ErrorCode.DUPLICATE_REPORT);
        }
    }
}
