package com.intelliguard.userservice.userservice.controller;

import com.intelliguard.userservice.userservice.entity.User;
import com.intelliguard.userservice.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("api/v1/users")
public class UserController {

    private UserService userService;
    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("all")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getUsers();
        if (users.size()!=0) return new ResponseEntity<>(users,HttpStatus.OK);
        return new ResponseEntity<>(null,HttpStatus.NO_CONTENT);
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        return new ResponseEntity<>(new User(),HttpStatus.CREATED);
    }

    @GetMapping("{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user=userService.getUserById(id);
        if(user!=null) return new ResponseEntity<>(user,HttpStatus.OK);
        else return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);
    }



}
