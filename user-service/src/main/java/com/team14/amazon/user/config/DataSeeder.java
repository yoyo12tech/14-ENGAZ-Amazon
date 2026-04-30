package com.team14.amazon.user.config;

import com.team14.amazon.user.model.User;
import com.team14.amazon.user.model.UserRole;
import com.team14.amazon.user.model.UserStatus;
import com.team14.amazon.user.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!userRepository.existsByRole(UserRole.ADMIN)) {
            User admin = new User();
            admin.setName("Admin User");
            admin.setEmail("admin@amazon.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setPhone("+201000000000");
            admin.setRole(UserRole.ADMIN);
            admin.setStatus(UserStatus.ACTIVE);
            userRepository.save(admin);
            System.out.println(">>> Seeded ADMIN user: admin@amazon.com / admin123");
        }
    }
}
