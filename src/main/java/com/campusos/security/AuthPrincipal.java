package com.campusos.security;

/** Principal stored in the SecurityContext for JWT-authenticated requests. */
public record AuthPrincipal(Long id, String email, String role) {
}
