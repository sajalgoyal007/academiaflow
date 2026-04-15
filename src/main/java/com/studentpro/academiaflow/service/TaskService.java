package com.studentpro.academiaflow.service;

import com.studentpro.academiaflow.model.Project;
import com.studentpro.academiaflow.model.Task;
import com.studentpro.academiaflow.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    public List<Task> getTasksByProject(Project project) {
        return taskRepository.findByProjectOrderByCreatedAtDesc(project);
    }

    public Task getTaskById(Long id) {
        return taskRepository.findById(id).orElse(null);
    }

    public Task saveTask(Task task) {
        return taskRepository.save(task);
    }

    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }

    @Transactional
    public void deleteTasksByProject(Project project) {
        List<Task> tasks = taskRepository.findByProject(project);
        taskRepository.deleteAll(tasks);
    }
}
