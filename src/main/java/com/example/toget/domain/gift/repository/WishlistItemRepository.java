package com.example.toget.domain.gift.repository;

import com.example.toget.domain.gift.entity.WishlistItem;
import com.example.toget.domain.gift.enums.WishlistType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
    Slice<WishlistItem> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);
    Slice<WishlistItem> findByUserIdOrderByIdAsc(Long userId, Pageable pageable);
    Slice<WishlistItem> findByUserIdAndTypeOrderByIdDesc(Long userId, WishlistType type, Pageable pageable);
    Slice<WishlistItem> findByUserIdAndTypeOrderByIdAsc(Long userId, WishlistType type, Pageable pageable);
}
