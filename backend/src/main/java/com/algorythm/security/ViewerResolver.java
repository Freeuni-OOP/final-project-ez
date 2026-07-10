package com.algorythm.security;

import com.algorythm.model.User;
import com.algorythm.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Single source of truth for "who is the current viewer" on optional-auth reads:
 * the username from an Authentication (null when anonymous), and the resolution
 * of a username to its user id (null when there is no viewer or they no longer
 * exist). Replaces the copies that used to live in the public controllers and in
 * the like/profile services.
 */
@Component
public class ViewerResolver {

    private final UserRepository users;

    public ViewerResolver(UserRepository users) {
        this.users = users;
    }

    /** The logged-in username, or null when the request is anonymous. */
    public String username(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication.getName();
    }

    /** The viewer's id, or null when there is no viewer or they no longer exist. */
    public Long resolveId(String username) {
        if (username == null) {
            return null;
        }
        return users.findByUsername(username).map(User::getId).orElse(null);
    }
}
