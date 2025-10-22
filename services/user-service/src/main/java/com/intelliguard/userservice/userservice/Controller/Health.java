package com.intelliguard.userservice.userservice.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class Health {
    @GetMapping("health")
    public ResponseEntity<?> getHeath(){
        return ResponseEntity.ok().build();
    }
}

