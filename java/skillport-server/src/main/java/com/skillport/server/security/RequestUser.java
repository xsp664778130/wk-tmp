package com.skillport.server.security;

public record RequestUser(String userId, String email, String displayName) {
}
