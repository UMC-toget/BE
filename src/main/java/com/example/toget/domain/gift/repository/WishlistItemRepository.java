package com.example.toget.domain.gift.repository;

import com.example.toget.domain.gift.entity.WishlistItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
    Slice<WishlistItem> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);
    Slice<WishlistItem> findByUserIdOrderByIdAsc(Long userId, Pageable pageable);
}
