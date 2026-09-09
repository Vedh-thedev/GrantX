package com.grantx.config;

import com.grantx.service.DataInitializationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationStartupRunner implements ApplicationRunner {

    private final DataInitializationService dataInitializationService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Grant-X Application starting up...");
        dataInitializationService.initializeSampleData();
        log.info("Grant-X Application ready!");
        log.info("=== DEFAULT CREDENTIALS ===");
        log.info("Admin:    admin / admin123");
        log.info("Faculty:  dr.priya / faculty123");
        log.info("Faculty:  dr.rajan / faculty123");
        log.info("Faculty:  dr.meena / faculty123");
        log.info("Student:  arjun.cs21 / student123");
        log.info("Student:  divya.ec22 / student123");
        log.info("Student:  rahul.mech21 / student123");
        log.info("===========================");
    }
}
