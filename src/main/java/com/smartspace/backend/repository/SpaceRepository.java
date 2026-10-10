package com.smartspace.backend.repository;

import com.smartspace.backend.model.Space;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpaceRepository extends JpaRepository<Space, Long> {
}