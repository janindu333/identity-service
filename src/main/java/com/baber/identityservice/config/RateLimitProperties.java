package com.baber.identityservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration holder for rate limiting thresholds applied to auth endpoints.
 */
@ConfigurationProperties(prefix = "security.rate-limit")
public class RateLimitProperties {

    private Endpoint login = new Endpoint();
    private Endpoint refresh = new Endpoint();

    public Endpoint getLogin() {
        return login;
    }

    public void setLogin(Endpoint login) {
        this.login = login;
    }

    public Endpoint getRefresh() {
        return refresh;
    }

    public void setRefresh(Endpoint refresh) {
        this.refresh = refresh;
    }

    public static class Endpoint {
        private Limit ip = new Limit();
        private Limit username = new Limit();

        public Limit getIp() {
            return ip;
        }

        public void setIp(Limit ip) {
            this.ip = ip;
        }

        public Limit getUsername() {
            return username;
        }

        public void setUsername(Limit username) {
            this.username = username;
        }
    }

    public static class Limit {
        /**
         * Maximum number of requests allowed inside the window.
         * Non-positive values disable the limit.
         */
        private int limit = 0;

        /**
         * Window size in seconds.
         */
        private long windowSeconds = 60;

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public long getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(long windowSeconds) {
            this.windowSeconds = windowSeconds;
        }
    }
}

