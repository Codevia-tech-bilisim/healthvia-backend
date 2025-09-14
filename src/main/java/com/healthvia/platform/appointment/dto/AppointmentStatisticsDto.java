// src/main/java/com/healthvia/platform/appointment/dto/AppointmentStatisticsDto.java
package com.healthvia.platform.appointment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentStatisticsDto {
    
    private long totalAppointments;
    private long pendingAppointments;
    private long confirmedAppointments;
    private long completedAppointments;
    private long cancelledAppointments;
    private double completionRate;
    private double noShowRate;
    
    // Builder pattern ile kolay kullanım:
    // AppointmentStatisticsDto.builder()
    //     .totalAppointments(100)
    //     .pendingAppointments(20)
    //     .build();
}