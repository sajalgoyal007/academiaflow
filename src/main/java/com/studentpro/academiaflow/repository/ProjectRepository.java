package com.studentpro.academiaflow.repository;

import com.studentpro.academiaflow.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
}
