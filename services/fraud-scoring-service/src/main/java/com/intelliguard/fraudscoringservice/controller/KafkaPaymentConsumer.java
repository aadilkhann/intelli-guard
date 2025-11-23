package com.intelliguard.fraudscoringservice.controller;

import com.intelliguard.fraudscoringservice.DTO.Transaction;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaPaymentConsumer {
    @KafkaListener(topics = "pending-payment-pool", groupId = "fraud-group")
    public void consumePendingPayment(Transaction transaction) {
        System.out.println("##---------------------------------------------------------------------------##");
        System.out.println(transaction);
        System.out.println("##---------------------------------------------------------------------------##");
    }
}
