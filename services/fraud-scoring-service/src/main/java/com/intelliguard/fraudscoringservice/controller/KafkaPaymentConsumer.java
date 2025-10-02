package com.intelliguard.fraudscoringservice.controller;

import com.intelliguard.fraudscoringservice.DTO.Transction;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaPaymentConsumer {
    @KafkaListener(topics = "pending-payment-pool", groupId = "fraud-group")
    public void consumePendingPayment(Transction transction) {
        System.out.println("##---------------------------------------------------------------------------##");
        System.out.println(transction);
        System.out.println("##---------------------------------------------------------------------------##");
    }
}
