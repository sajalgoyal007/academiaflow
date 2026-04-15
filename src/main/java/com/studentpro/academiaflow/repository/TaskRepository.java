package com.studentpro.academiaflow.repository;

import com.studentpro.academiaflow.model.Task;
import com.studentpro.academiaflow.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProject(Project project);
    List<Task> findByProjectOrderByCreatedAtDesc(Project project);
    
    @Query("SELECT COUNT(t) FROM Task t, ProjectMember pm WHERE t.project = pm.project AND pm.user.id = :userId")
    long countAllTasksForUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(t) FROM Task t, ProjectMember pm WHERE t.project = pm.project AND pm.user.id = :userId AND t.status = 'COMPLETED'")
    long countCompletedTasksForUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(t) FROM Task t, ProjectMember pm WHERE t.project = pm.project AND pm.user.id = :userId AND (t.status = 'TODO' OR t.status = 'IN_PROGRESS' OR t.status = 'REVIEW')")
    long countPendingTasksForUser(@Param("userId") Long userId);

    // Daily productivity: tasks completed today
    @Query("SELECT COUNT(t) FROM Task t, ProjectMember pm WHERE t.project = pm.project AND pm.user.id = :userId AND t.status = 'COMPLETED' AND t.createdAt >= :startOfDay")
    long countCompletedTodayForUser(@Param("userId") Long userId, @Param("startOfDay") LocalDateTime startOfDay);

    // Tasks created today
    @Query("SELECT COUNT(t) FROM Task t, ProjectMember pm WHERE t.project = pm.project AND pm.user.id = :userId AND t.createdAt >= :startOfDay")
    long countCreatedTodayForUser(@Param("userId") Long userId, @Param("startOfDay") LocalDateTime startOfDay);

    // All tasks for user (for report)
    @Query("SELECT t FROM Task t, ProjectMember pm WHERE t.project = pm.project AND pm.user.id = :userId ORDER BY t.project.name, t.status")
    List<Task> findAllTasksForUser(@Param("userId") Long userId);
}
