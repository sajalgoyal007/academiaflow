package com.studentpro.academiaflow.service;

import com.studentpro.academiaflow.model.User;
import com.studentpro.academiaflow.repository.UserRepository;
import com.studentpro.academiaflow.util.HashUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User registerUser(String name, String email, String password) throws Exception {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new Exception("Email already exists");
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(HashUtil.hashPassword(password));
        return userRepository.save(user);
    }

    public User authenticate(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (HashUtil.checkPassword(password, user.getPasswordHash())) {
                return user;
            }
        }
        return null;
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
    
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public User updateProfile(User user) {
        return userRepository.save(user);
    }

    public boolean changePassword(User user, String oldPassword, String newPassword) {
        if (HashUtil.checkPassword(oldPassword, user.getPasswordHash())) {
            user.setPasswordHash(HashUtil.hashPassword(newPassword));
            userRepository.save(user);
            return true;
        }
        return false;
    }
}
