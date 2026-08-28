package com.playops.api.repository;

import com.playops.api.entity.BoardPost;
import com.playops.api.entity.BoardPostType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardPostRepository extends JpaRepository<BoardPost, Long> {
    List<BoardPost> findByType(BoardPostType type, Sort sort);
    List<BoardPost> findByVisibleTrue(Sort sort);
    List<BoardPost> findByTypeAndVisibleTrue(BoardPostType type, Sort sort);
    List<BoardPost> findByParentId(Long parentId, Sort sort);
    long countByParentId(Long parentId);
}
