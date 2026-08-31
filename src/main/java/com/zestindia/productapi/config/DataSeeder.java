package com.zestindia.productapi.config;

import com.zestindia.productapi.auth.Role;
import com.zestindia.productapi.auth.UserAccount;
import com.zestindia.productapi.auth.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    public DataSeeder(UserAccountRepository userAccountRepository,
                      PasswordEncoder passwordEncoder,
                      AppProperties appProperties) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.appProperties = appProperties;
    }

    @Override
    public void run(String... args) {
        AppProperties.Seed seed = appProperties.getSeed();
        createIfMissing(seed.getAdminUsername(), seed.getAdminEmail(), seed.getAdminPassword(), Role.ADMIN);
        createIfMissing(seed.getUserUsername(), seed.getUserEmail(), seed.getUserPassword(), Role.USER);
    }

    private void createIfMissing(String username, String email, String password, Role role) {
        if (userAccountRepository.existsByUsername(username)) {
            return;
        }
        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setEmail(email);
        account.setPassword(passwordEncoder.encode(password));
        account.setRole(role);
        account.setEnabled(true);
        account.setCreatedOn(Instant.now());
        userAccountRepository.save(account);
        log.info("Seeded {} account: {}", role, username);
    }
}
