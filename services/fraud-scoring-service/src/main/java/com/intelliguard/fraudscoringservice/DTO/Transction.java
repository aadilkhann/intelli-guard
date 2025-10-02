package com.intelliguard.fraudscoringservice.DTO;

import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Transction {
    private String transactionId;
    private String userId;
    private double amount;
    private String location;
    private String deviceId;
    LocalDateTime timestamp;
}
