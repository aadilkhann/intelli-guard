package com.intelliguard.userservice.userservice.service;

import com.intelliguard.userservice.userservice.entity.User;
import com.intelliguard.userservice.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    UserRepository userRepository;
    @Autowired
    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    @Override
    public User getUserById(Long id) {
        User user = userRepository.findById(id).orElse(null);
        return user;
    }

    @Override
    public List<User> getUsers() {
        return (List<User>) userRepository.findAll();
    }

    @Override
    public User createUser(User user) {
        return userRepository.save(user);
    }
}
