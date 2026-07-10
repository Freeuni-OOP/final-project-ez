package com.algorythm.config;

import com.algorythm.model.Role;
import com.algorythm.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Promotes the usernames listed in app.admin.usernames to ADMIN on startup, so
 * there's a way to bootstrap the first admin (set APP_ADMIN_USERNAMES in the
 * environment). Users that don't exist yet are skipped.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository users;
    private final String configured;

    public AdminBootstrap(UserRepository users,
                          @Value("${app.admin.usernames:}") String configured) {
        this.users = users;
        this.configured = configured;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (configured == null || configured.isBlank()) {
            return;
        }
        for (String raw : configured.split(",")) {
            String username = raw.trim();
            if (username.isEmpty()) {
                continue;
            }
            users.findByUsername(username).ifPresent(user -> {
                if (user.getRole() != Role.ADMIN) {
                    user.setRole(Role.ADMIN);
                    log.info("Promoted user '{}' to ADMIN via app.admin.usernames", username);
                }
            });
        }
    }
}
