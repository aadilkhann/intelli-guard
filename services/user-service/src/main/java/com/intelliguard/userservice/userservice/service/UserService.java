package com.intelliguard.userservice.userservice.service;

import com.intelliguard.userservice.userservice.entity.User;

import java.util.List;

public interface UserService {
    User getUserById(Long id);
    List<User> getUsers();
    User createUser(User user);
}
