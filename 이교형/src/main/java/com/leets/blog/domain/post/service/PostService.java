package com.leets.blog.domain.post.service;

import com.leets.blog.common.exception.CustomException;
import com.leets.blog.common.exception.ErrorCode;
import com.leets.blog.domain.post.entity.Post;
import com.leets.blog.domain.post.repository.PostRepository;
import com.leets.blog.domain.post.web.dto.request.PostSortType;
import com.leets.blog.domain.post.web.dto.response.PostDetailResponse;
import com.leets.blog.domain.post.web.dto.response.PostListResponse;
import com.leets.blog.domain.post.web.dto.response.PostSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 50;

    private final PostRepository postRepository;

    // 게시글 목록 조회
    public PostListResponse getPosts(int page, int size, String sort) {
        validatePage(page, size);
        PostSortType sortType = PostSortType.from(sort);

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<PostSummaryResponse> result = switch (sortType) {
            case LATEST -> postRepository.findLatest(pageable);
            case OLDEST -> postRepository.findOldest(pageable);
            case POPULAR -> postRepository.findPopular(pageable);
        };
        return PostListResponse.from(result);
    }

    // 게시글 상세 조회
    public PostDetailResponse getPost(Long postId) {
        return PostDetailResponse.from(findPost(postId));
    }

    private void validatePage(int page, int size) {
        if (page < 1) {
            throw new CustomException(ErrorCode.INVALID_PAGE_NUMBER);
        }
        if (size < MIN_PAGE_SIZE || size > MAX_PAGE_SIZE) {
            throw new CustomException(ErrorCode.INVALID_PAGE_SIZE);
        }
    }

    // 0 이하 ID는 400, 없거나 삭제된 글은 404
    private Post findPost(Long postId) {
        if (postId < 1) {
            throw new CustomException(ErrorCode.INVALID_POST_ID);
        }
        return postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
    }
}
