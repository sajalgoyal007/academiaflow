package com.studentpro.academiaflow.repository;

import com.studentpro.academiaflow.model.ProjectMember;
import com.studentpro.academiaflow.model.Project;
import com.studentpro.academiaflow.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    List<ProjectMember> findByUser(User user);
    List<ProjectMember> findByProject(Project project);
    Optional<ProjectMember> findByProjectAndUser(Project project, User user);
    long countByUser(User user);
}
