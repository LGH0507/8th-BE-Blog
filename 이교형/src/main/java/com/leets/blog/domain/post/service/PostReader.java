package com.leets.blog.domain.post.service;

import com.leets.blog.common.exception.CustomException;
import com.leets.blog.common.exception.ErrorCode;
import com.leets.blog.domain.post.entity.Post;
import com.leets.blog.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 여러 서비스가 함께 쓰는 게시글 조회 (호출한 서비스의 트랜잭션 안에서 실행)
@Component
@RequiredArgsConstructor
public class PostReader {

    private final PostRepository postRepository;

    // 0 이하 ID는 400, 없거나 삭제·숨김된 글은 404 (숨김 글은 작성자에게도 없는 글로 취급)
    public Post getVisiblePost(Long postId) {
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
}
