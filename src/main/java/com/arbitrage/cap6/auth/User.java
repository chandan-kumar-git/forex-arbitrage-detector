package com.arbitrage.cap6.auth;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A portal user as kept in the HttpSession. It is a JavaBean (getters) so JSP EL can read it,
 * and Serializable because session objects may be written to disk by the servlet container.
 * The password hash is never put in the session.
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long id;
    private final String username;
    private final String displayName;
    private final LocalDateTime lastLoginAt;

    public User(long id, String username, String displayName, LocalDateTime lastLoginAt) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.lastLoginAt = lastLoginAt;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** When this user logged in before the current login, or null on the first login. */
    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }
}
