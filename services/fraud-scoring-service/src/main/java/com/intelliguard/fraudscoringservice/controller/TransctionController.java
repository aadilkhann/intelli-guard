package com.intelliguard.fraudscoringservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class TransctionController {
    @PostMapping("/transfer")
    public ResponseEntity<String> transfer(@RequestParam("from") String from, @RequestParam("to") String to) {

    }
}
