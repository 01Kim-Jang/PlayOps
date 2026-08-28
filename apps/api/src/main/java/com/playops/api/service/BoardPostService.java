package com.playops.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playops.api.dto.BoardAttachment;
import com.playops.api.dto.BoardPostRequest;
import com.playops.api.dto.BoardPostCommentRequest;
import com.playops.api.dto.BoardPostCommentResponse;
import com.playops.api.dto.BoardPostReactionRequest;
import com.playops.api.dto.BoardPostResponse;
import com.playops.api.entity.BoardPost;
import com.playops.api.entity.BoardPostComment;
import com.playops.api.entity.BoardPostReaction;
import com.playops.api.entity.BoardPostType;
import com.playops.api.entity.User;
import com.playops.api.entity.UserRole;
import com.playops.api.exception.ApiException;
import com.playops.api.repository.BoardPostCommentRepository;
import com.playops.api.repository.BoardPostReactionRepository;
import com.playops.api.repository.BoardPostRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class BoardPostService {

    private static final Sort DEFAULT_SORT = Sort.by(
            Sort.Order.desc("pinned"),
            Sort.Order.desc("createdAt")
    );
    private static final int MAX_ATTACHMENTS = 5;
    private static final long MAX_ATTACHMENT_SIZE = 5 * 1024 * 1024;
    private static final TypeReference<List<BoardAttachment>> ATTACHMENT_LIST_TYPE = new TypeReference<>() {};

    private final BoardPostRepository repository;
    private final BoardPostCommentRepository commentRepository;
    private final BoardPostReactionRepository reactionRepository;
    private final ObjectMapper objectMapper;

    public BoardPostService(
            BoardPostRepository repository,
            BoardPostCommentRepository commentRepository,
            BoardPostReactionRepository reactionRepository,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.commentRepository = commentRepository;
        this.reactionRepository = reactionRepository;
        this.objectMapper = objectMapper;
    }

    public List<BoardPostResponse> findAll(BoardPostType type, boolean includeHidden, User user) {
        List<BoardPost> posts;
        if (type != null && includeHidden) {
            posts = repository.findByType(type, DEFAULT_SORT);
        } else if (type != null) {
            posts = repository.findByTypeAndVisibleTrue(type, DEFAULT_SORT);
        } else if (includeHidden) {
            posts = repository.findAll(DEFAULT_SORT);
        } else {
            posts = repository.findByVisibleTrue(DEFAULT_SORT);
        }
        return posts.stream()
                .filter(post -> includeHidden || isActive(post, Instant.now()))
                .map(post -> toResponse(post, user))
                .toList();
    }

    public List<BoardPostResponse> visibleNotices(User user) {
        Instant now = Instant.now();
        return repository.findByTypeAndVisibleTrue(BoardPostType.NOTICE, DEFAULT_SORT)
                .stream()
                .filter(post -> post.getParentId() == null)
                .filter(post -> isActive(post, now))
                .map(this::toNoticeResponse)
                .toList();
    }

    public BoardPostResponse create(BoardPostRequest request, User user) {
        BoardPost post = new BoardPost();
        applyRequest(post, request, isAdmin(user));
        post.setCreatedBy(user.getUsername());
        post.setUpdatedBy(user.getUsername());
        return toResponse(repository.save(post), user);
    }

    public BoardPostResponse update(Long id, BoardPostRequest request, User user) {
        BoardPost post = repository.findById(id)
                .orElseThrow(() -> new ApiException(404, "Post not found"));
        if (!canEdit(post, user)) {
            throw new ApiException(403, "Post edit is not allowed");
        }
        applyRequest(post, request, isAdmin(user));
        post.setUpdatedBy(user.getUsername());
        return toResponse(repository.save(post), user);
    }

    @Transactional
    public void delete(Long id, User user) {
        BoardPost post = repository.findById(id)
                .orElseThrow(() -> new ApiException(404, "Post not found"));
        if (!canEdit(post, user)) {
            throw new ApiException(403, "Post delete is not allowed");
        }
        deletePostTree(post);
    }

    public List<BoardPostCommentResponse> comments(Long postId) {
        ensurePost(postId);
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId)
                .stream()
                .map(comment -> BoardPostCommentResponse.from(comment, readAttachments(comment.getAttachmentsJson())))
                .toList();
    }

    public BoardPostCommentResponse createComment(Long postId, BoardPostCommentRequest request, User user) {
        BoardPost post = ensurePost(postId);
        String content = request != null && request.content() != null ? request.content().trim() : "";
        if (content.isBlank()) {
            throw new ApiException(400, "Comment content is required");
        }
        Long parentId = request != null ? request.parentId() : null;
        if (parentId != null) {
            BoardPostComment parent = commentRepository.findById(parentId)
                    .orElseThrow(() -> new ApiException(404, "Parent comment not found"));
            if (!parent.getPost().getId().equals(postId)) {
                throw new ApiException(400, "Parent comment belongs to another post");
            }
        }
        BoardPostComment comment = new BoardPostComment();
        comment.setPost(post);
        comment.setParentId(parentId);
        comment.setContent(content);
        comment.setAttachmentsJson(writeAttachments(request != null ? request.attachments() : null));
        comment.setCreatedBy(user.getUsername());
        comment.setUpdatedBy(user.getUsername());
        BoardPostComment saved = commentRepository.save(comment);
        return BoardPostCommentResponse.from(saved, readAttachments(saved.getAttachmentsJson()));
    }

    public void deleteComment(Long commentId, User user) {
        BoardPostComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ApiException(404, "Comment not found"));
        if (!isAdmin(user) && !comment.getCreatedBy().equals(user.getUsername())) {
            throw new ApiException(403, "Comment delete is not allowed");
        }
        commentRepository.delete(comment);
    }

    public BoardPostResponse updateReaction(Long postId, BoardPostReactionRequest request, User user) {
        BoardPost post = ensurePost(postId);
        BoardPostReaction reaction = reactionRepository.findByPostIdAndUsername(postId, user.getUsername())
                .orElseGet(BoardPostReaction::new);
        reaction.setPost(post);
        reaction.setUsername(user.getUsername());
        reaction.setLiked(request != null && Boolean.TRUE.equals(request.liked()));
        reaction.setRating(normalizeRating(request != null ? request.rating() : null));
        reaction.setMemo(normalizeMemo(request != null ? request.memo() : null));
        reactionRepository.save(reaction);
        return toResponse(post, user);
    }

    private void applyRequest(BoardPost post, BoardPostRequest request, boolean admin) {
        if (request == null) {
            throw new ApiException(400, "Post request is required");
        }
        String title = request.title() != null ? request.title().trim() : "";
        String content = request.content() != null ? request.content().trim() : "";
        if (title.isBlank()) {
            throw new ApiException(400, "Title is required");
        }
        if (content.isBlank()) {
            throw new ApiException(400, "Content is required");
        }
        BoardPostType type = request.type() != null ? request.type() : BoardPostType.FREE;
        if (!admin && type == BoardPostType.NOTICE) {
            type = BoardPostType.FREE;
        }
        post.setType(type);
        post.setTitle(title);
        post.setParentId(resolveParentId(post, request.parentId()));
        post.setContent(content);
        post.setAttachmentsJson(writeAttachments(request.attachments()));
        if (admin) {
            post.setVisible(Boolean.TRUE.equals(request.visible()));
            post.setPinned(Boolean.TRUE.equals(request.pinned()));
            post.setVisibleFrom(request.visibleFrom());
            post.setVisibleUntil(request.visibleUntil());
            if (post.getVisibleFrom() != null
                    && post.getVisibleUntil() != null
                    && post.getVisibleFrom().isAfter(post.getVisibleUntil())) {
                throw new ApiException(400, "Visible from must be before visible until");
            }
        } else {
            post.setVisible(true);
            post.setPinned(false);
            post.setVisibleFrom(null);
            post.setVisibleUntil(null);
        }
    }

    private BoardPost ensurePost(Long postId) {
        return repository.findById(postId)
                .orElseThrow(() -> new ApiException(404, "Post not found"));
    }

    private BoardPostResponse toResponse(BoardPost post, User user) {
        BoardPostReaction myReaction = user != null
                ? reactionRepository.findByPostIdAndUsername(post.getId(), user.getUsername()).orElse(null)
                : null;
        return BoardPostResponse.from(
                post,
                reactionRepository.countByPostIdAndLikedTrue(post.getId()),
                reactionRepository.averageRatingByPostId(post.getId()),
                reactionRepository.countByPostIdAndRatingIsNotNull(post.getId()),
                commentRepository.countByPostId(post.getId()),
                repository.countByParentId(post.getId()),
                myReaction != null && myReaction.getLiked(),
                myReaction != null ? myReaction.getRating() : null,
                myReaction != null ? myReaction.getMemo() : null,
                readAttachments(post.getAttachmentsJson())
        );
    }

    private BoardPostResponse toNoticeResponse(BoardPost post) {
        return BoardPostResponse.from(
                post,
                0L,
                null,
                0L,
                0L,
                0L,
                false,
                null,
                null,
                readAttachments(post.getAttachmentsJson())
        );
    }

    private List<BoardAttachment> readAttachments(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<BoardAttachment> attachments = objectMapper.readValue(json, ATTACHMENT_LIST_TYPE);
            return attachments != null ? attachments : List.of();
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private String writeAttachments(List<BoardAttachment> attachments) {
        List<BoardAttachment> normalized = normalizeAttachments(attachments);
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (JsonProcessingException e) {
            throw new ApiException(400, "Attachment data is invalid");
        }
    }

    private List<BoardAttachment> normalizeAttachments(List<BoardAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return Collections.emptyList();
        }
        if (attachments.size() > MAX_ATTACHMENTS) {
            throw new ApiException(400, "Attachments are limited to 5 files");
        }
        List<BoardAttachment> normalized = new ArrayList<>();
        for (BoardAttachment attachment : attachments) {
            if (attachment == null) {
                continue;
            }
            String name = attachment.name() != null ? attachment.name().trim() : "";
            String dataUrl = attachment.dataUrl() != null ? attachment.dataUrl().trim() : "";
            long size = attachment.size() != null ? attachment.size() : 0;
            if (name.isBlank() || dataUrl.isBlank()) {
                continue;
            }
            if (size < 0 || size > MAX_ATTACHMENT_SIZE) {
                throw new ApiException(400, "Each attachment must be 5 MiB or less");
            }
            if (!dataUrl.startsWith("data:")) {
                throw new ApiException(400, "Attachment data is invalid");
            }
            String safeName = name.length() > 180 ? name.substring(0, 180) : name;
            String contentType = attachment.contentType() != null && !attachment.contentType().isBlank()
                    ? attachment.contentType().trim()
                    : "application/octet-stream";
            normalized.add(new BoardAttachment(safeName, contentType, size, dataUrl));
        }
        return normalized;
    }

    private boolean isActive(BoardPost post, Instant now) {
        if (!post.getVisible()) {
            return false;
        }
        if (post.getVisibleFrom() != null && now.isBefore(post.getVisibleFrom())) {
            return false;
        }
        return post.getVisibleUntil() == null || !now.isAfter(post.getVisibleUntil());
    }

    private boolean canEdit(BoardPost post, User user) {
        return isAdmin(user) || post.getCreatedBy().equals(user.getUsername());
    }

    private Long resolveParentId(BoardPost post, Long parentId) {
        if (parentId == null) {
            return null;
        }
        if (post.getId() != null && post.getId().equals(parentId)) {
            throw new ApiException(400, "Post cannot be a reply to itself");
        }
        BoardPost parent = repository.findById(parentId)
                .orElseThrow(() -> new ApiException(404, "Parent post not found"));
        if (post.getId() != null && isDescendant(parent, post.getId())) {
            throw new ApiException(400, "Post cannot be moved under its reply");
        }
        return parent.getId();
    }

    private boolean isDescendant(BoardPost post, Long ancestorId) {
        Long parentId = post.getParentId();
        while (parentId != null) {
            if (parentId.equals(ancestorId)) {
                return true;
            }
            BoardPost parent = repository.findById(parentId).orElse(null);
            parentId = parent != null ? parent.getParentId() : null;
        }
        return false;
    }

    private void deletePostTree(BoardPost post) {
        repository.findByParentId(post.getId(), DEFAULT_SORT).forEach(this::deletePostTree);
        commentRepository.deleteByPostId(post.getId());
        reactionRepository.deleteByPostId(post.getId());
        repository.delete(post);
    }

    private boolean isAdmin(User user) {
        return user != null && user.getRole() == UserRole.ADMIN;
    }

    private Integer normalizeRating(Integer rating) {
        if (rating == null) {
            return null;
        }
        if (rating < 1 || rating > 5) {
            throw new ApiException(400, "Rating must be between 1 and 5");
        }
        return rating;
    }

    private String normalizeMemo(String memo) {
        if (memo == null) {
            return "";
        }
        String trimmed = memo.trim();
        return trimmed.length() > 2000 ? trimmed.substring(0, 2000) : trimmed;
    }
}
