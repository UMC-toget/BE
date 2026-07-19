package com.example.toget.domain.funding.repository;

import com.example.toget.domain.funding.entity.ContributionBackground;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContributionBackgroundRepository extends JpaRepository<ContributionBackground, Long> {

}