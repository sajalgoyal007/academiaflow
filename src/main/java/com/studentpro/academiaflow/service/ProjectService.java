package com.studentpro.academiaflow.service;

import com.studentpro.academiaflow.model.Project;
import com.studentpro.academiaflow.model.ProjectMember;
import com.studentpro.academiaflow.model.User;
import com.studentpro.academiaflow.repository.ProjectMemberRepository;
import com.studentpro.academiaflow.repository.ProjectRepository;
import com.studentpro.academiaflow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository memberRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public Project createProject(Project project, User owner) {
        Project savedProject = projectRepository.save(project);
        
        ProjectMember ownerMember = new ProjectMember();
        ownerMember.setProject(savedProject);
        ownerMember.setUser(owner);
        ownerMember.setRole("OWNER");
        memberRepository.save(ownerMember);

        return savedProject;
    }

    public List<ProjectMember> getUserProjects(User user) {
        return memberRepository.findByUser(user);
    }

    public Project getProjectById(Long id) {
        return projectRepository.findById(id).orElse(null);
    }
    
    public ProjectMember getMemberRole(Project project, User user) {
        return memberRepository.findByProjectAndUser(project, user).orElse(null);
    }

    public List<ProjectMember> getProjectMembers(Project project) {
        return memberRepository.findByProject(project);
    }

    @Transactional
    public void addMemberByEmail(Long projectId, String email, String role) throws Exception {
        Project project = getProjectById(projectId);
        if (project == null) throw new Exception("Project not found");

        User user = userRepository.findByEmail(email).orElseThrow(() -> new Exception("User not found with email " + email));

        Optional<ProjectMember> existingMember = memberRepository.findByProjectAndUser(project, user);
        if (existingMember.isPresent()) {
            throw new Exception("User is already a member of this project");
        }

        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(user);
        member.setRole(role); // OWNER, MEMBER, VIEWER
        memberRepository.save(member);
    }

    public void deleteProject(Long id) {
        // Dependencies must be deleted or cascaded (tasks, members)
        Project project = getProjectById(id);
        if (project != null) {
            List<ProjectMember> members = memberRepository.findByProject(project);
            memberRepository.deleteAll(members);
            projectRepository.delete(project);
        }
    }
    
    public Project updateProject(Project project) {
        return projectRepository.save(project);
    }
}
