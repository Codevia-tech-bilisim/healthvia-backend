package com.healthvia.platform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @Scheduled annotation'ları aktifleştirir.
 * ReminderServiceImpl içindeki cron job'lar bu config sayesinde çalışır.
 *
 * Production'da Cloud Run min-instances=1 olmalı, aksi halde cold start
 * durumunda scheduled job'lar çalışmaz.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
