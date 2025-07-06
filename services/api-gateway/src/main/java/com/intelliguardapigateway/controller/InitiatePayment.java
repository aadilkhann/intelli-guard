package com.intelliguardapigateway.controller;

import com.intelliguardapigateway.service.KafkaProducer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class InitiatePayment {

    private final KafkaProducer kafkaProducer;

    public InitiatePayment(KafkaProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    @PostMapping("initiate-payment")
    public ResponseEntity<String> initiatePayment(@RequestBody String payload) {
        kafkaProducer.sendMessage("pending-payment-pool", payload);
        return ResponseEntity.ok("Payment intiated");
    }
}
