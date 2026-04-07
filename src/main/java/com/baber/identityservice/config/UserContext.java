package com.baber.identityservice.config;

public class UserContext {
    private static final ThreadLocal<String> userDetailsJsonHolder = new ThreadLocal<>();

    public static void setUserDetailsJson(String userDetailsJson) {
        userDetailsJsonHolder.set(userDetailsJson);
    }

    public static String getUserDetailsJson() {
        return userDetailsJsonHolder.get();
    }

    public static void clear() {
        userDetailsJsonHolder.remove();
    }
}

