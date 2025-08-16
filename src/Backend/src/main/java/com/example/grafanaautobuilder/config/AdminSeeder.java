package com.example.grafanaautobuilder.config;

import com.example.grafanaautobuilder.entity.User;
import com.example.grafanaautobuilder.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminSeeder {
    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    @Bean
    CommandLineRunner seedAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            final String adminEmail = "admin@gmail.com";
            if (userRepository.existsByEmail(adminEmail)) {
                log.info("Admin user already exists: {}", adminEmail);
                return;
            }
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setEnabled(true);
            // roles default to ROLE_USER in entity; guard uses email equality
            userRepository.save(admin);
            log.info("Seeded default admin user: {} with password: {}", adminEmail, "Admin@123");
        };
    }
}
