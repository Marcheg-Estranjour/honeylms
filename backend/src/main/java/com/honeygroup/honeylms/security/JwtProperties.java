package com.honeygroup.honeylms.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from `app.jwt.*` in application.yml.
 * See Dossier de Conception §12.2 : expiration fixe, pas de refresh token pour le MVP.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expirationMs) {
}
