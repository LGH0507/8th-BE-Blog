package com.leets.blog.domain.post.web.controller;

import com.leets.blog.common.exception.BindErrorCode;
import com.leets.blog.common.exception.ErrorCode;
import com.leets.blog.common.response.ApiResponse;
import com.leets.blog.domain.post.service.PostService;
import com.leets.blog.domain.post.web.dto.response.PostDetailResponse;
import com.leets.blog.domain.post.web.dto.response.PostListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Post API", description = "게시글 CRUD API")
@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @Operation(summary = "게시글 목록 조회", description = "삭제되지 않은 게시글을 페이지 단위로 조회합니다. sort: latest, oldest, popular")
    @GetMapping
    public ResponseEntity<ApiResponse<PostListResponse>> getPosts(
            @RequestParam(defaultValue = "1") @BindErrorCode(ErrorCode.INVALID_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = "10") @BindErrorCode(ErrorCode.INVALID_PAGE_SIZE) int size,
            @RequestParam(defaultValue = "latest") String sort) {
        return ApiResponse.okEntity(postService.getPosts(page, size, sort));
    }

    @Operation(summary = "게시글 상세 조회", description = "삭제되지 않은 게시글 하나를 조회합니다.")
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> getPost(
            @PathVariable @BindErrorCode(ErrorCode.INVALID_POST_ID) Long postId) {
        return ApiResponse.okEntity(postService.getPost(postId));
    }
}
