package com.leets.blog.domain.report.service;

import com.leets.blog.common.exception.CustomException;
import com.leets.blog.common.exception.ErrorCode;
import com.leets.blog.domain.comment.entity.Comment;
import com.leets.blog.domain.comment.repository.CommentRepository;
import com.leets.blog.domain.member.entity.Member;
import com.leets.blog.domain.member.repository.MemberRepository;
import com.leets.blog.domain.post.entity.Post;
import com.leets.blog.domain.post.repository.PostRepository;
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
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    // 게시글 신고: 회원(401) → 사유(400) → 게시글 ID(400) → 게시글 존재(404) → 본인 글(400) → 중복(409)
    @Transactional
    public ReportResponse reportPost(Long memberId, Long postId, ReportCreateRequest request) {
        Member reporter = findMember(memberId);
        ReportReason reason = ReportReason.from(request.reason());
        Post post = findPost(postId);
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
        Member reporter = findMember(memberId);
        ReportReason reason = ReportReason.from(request.reason());
        Comment comment = findComment(commentId);
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

    // X-Member-Id로 받은 회원 확인, 없거나 탈퇴한 회원이면 401
    private Member findMember(Long memberId) {
        if (memberId < 1) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
    }

    // 0 이하 ID는 400, 없거나 삭제·숨김된 글은 404
    private Post findPost(Long postId) {
        if (postId < 1) {
            throw new CustomException(ErrorCode.INVALID_POST_ID);
        }
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        if (post.isHidden()) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }
        return post;
    }

    // 0 이하 ID는 400, 없거나 삭제된 댓글·숨김 글의 댓글은 404
    // (글이 삭제되면 댓글도 함께 삭제되므로 부모 글의 삭제 여부는 따로 확인하지 않음)
    private Comment findComment(Long commentId) {
        if (commentId < 1) {
            throw new CustomException(ErrorCode.INVALID_COMMENT_ID);
        }
        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
        if (comment.getPost().isHidden()) {
            throw new CustomException(ErrorCode.COMMENT_NOT_FOUND);
        }
        return comment;
    }
}
