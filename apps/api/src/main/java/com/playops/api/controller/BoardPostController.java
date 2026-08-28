package com.playops.api.controller;

import com.playops.api.dto.BoardPostRequest;
import com.playops.api.dto.BoardPostCommentRequest;
import com.playops.api.dto.BoardPostCommentResponse;
import com.playops.api.dto.BoardPostReactionRequest;
import com.playops.api.dto.BoardPostResponse;
import com.playops.api.entity.BoardPostType;
import com.playops.api.entity.User;
import com.playops.api.entity.UserRole;
import com.playops.api.exception.ApiException;
import com.playops.api.service.BoardPostService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/board-posts")
public class BoardPostController {

    private final BoardPostService service;

    public BoardPostController(BoardPostService service) {
        this.service = service;
    }

    @GetMapping
    public List<BoardPostResponse> list(
            @RequestParam(required = false) BoardPostType type,
            @RequestParam(defaultValue = "false") boolean includeHidden,
            HttpServletRequest request
    ) {
        if (includeHidden) {
            requireAdmin(request);
        }
        return service.findAll(type, includeHidden, currentUser(request));
    }

    @GetMapping("/notices/visible")
    public List<BoardPostResponse> visibleNotices(HttpServletRequest request) {
        return service.visibleNotices(currentUser(request));
    }

    @PostMapping
    public BoardPostResponse create(@RequestBody BoardPostRequest body, HttpServletRequest request) {
        User user = currentUser(request);
        return service.create(body, user);
    }

    @PutMapping("/{id}")
    public BoardPostResponse update(
            @PathVariable Long id,
            @RequestBody BoardPostRequest body,
            HttpServletRequest request
    ) {
        User user = currentUser(request);
        return service.update(id, body, user);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, HttpServletRequest request) {
        service.delete(id, currentUser(request));
    }

    @GetMapping("/{id}/comments")
    public List<BoardPostCommentResponse> comments(@PathVariable Long id) {
        return service.comments(id);
    }

    @PostMapping("/{id}/comments")
    public BoardPostCommentResponse createComment(
            @PathVariable Long id,
            @RequestBody BoardPostCommentRequest body,
            HttpServletRequest request
    ) {
        return service.createComment(id, body, currentUser(request));
    }

    @DeleteMapping("/comments/{commentId}")
    public void deleteComment(@PathVariable Long commentId, HttpServletRequest request) {
        service.deleteComment(commentId, currentUser(request));
    }

    @PutMapping("/{id}/reaction")
    public BoardPostResponse updateReaction(
            @PathVariable Long id,
            @RequestBody BoardPostReactionRequest body,
            HttpServletRequest request
    ) {
        return service.updateReaction(id, body, currentUser(request));
    }

    private User currentUser(HttpServletRequest request) {
        User user = (User) request.getAttribute("currentUser");
        if (user == null) {
            throw new ApiException(401, "Authentication required");
        }
        return user;
    }

    private User requireAdmin(HttpServletRequest request) {
        User user = currentUser(request);
        if (user == null || user.getRole() != UserRole.ADMIN) {
            throw new ApiException(403, "Admin access required");
        }
        return user;
    }
}
