package com.reportplatform.auth.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class TokenValidationService {

    private static final Logger log = LoggerFactory.getLogger(TokenValidationService.class);

    private final String jwksUri;
    private final String expectedIssuer;
    private final String expectedAudience;
    private final Duration cacheTtl;

    private volatile JWKSet cachedJwkSet;
    private volatile Instant cacheExpiry = Instant.MIN;
    private final ReentrantReadWriteLock jwksLock = new ReentrantReadWriteLock();

    public TokenValidationService(
            @Value("${azure.entra.jwks-uri}") String jwksUri,
            @Value("${azure.entra.issuer}") String expectedIssuer,
            @Value("${azure.entra.client-id}") String expectedAudience,
            @Value("${auth.jwks.cache-ttl-minutes:5}") int cacheTtlMinutes) {
        this.jwksUri = jwksUri;
        this.expectedIssuer = expectedIssuer;
        this.expectedAudience = expectedAudience;
        this.cacheTtl = Duration.ofMinutes(cacheTtlMinutes);
    }

    /**
     * Validates an Azure Entra ID JWT v2 token and returns the claims.
     *
     * @param token the raw Bearer token (without "Bearer " prefix)
     * @return validated claims if token is valid, empty otherwise
     */
    public Optional<ValidatedClaims> validateToken(String token) {
        try {
            JWKSet jwkSet = getJwkSet();
            JWKSource<SecurityContext> keySource = new ImmutableJWKSet<>(jwkSet);

            ConfigurableJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
            JWSKeySelector<SecurityContext> keySelector =
                    new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource);
            processor.setJWSKeySelector(keySelector);

            JWTClaimsSet claims = processor.process(token, null);

            // Validate issuer
            if (!expectedIssuer.equals(claims.getIssuer())) {
                log.warn("Token issuer mismatch: expected={}, actual={}", expectedIssuer, claims.getIssuer());
                return Optional.empty();
            }

            // Validate audience
            List<String> audiences = claims.getAudience();
            if (audiences == null || !audiences.contains(expectedAudience)) {
                log.warn("Token audience mismatch: expected={}, actual={}", expectedAudience, audiences);
                return Optional.empty();
            }

            // Validate expiration
            if (claims.getExpirationTime() != null
                    && claims.getExpirationTime().toInstant().isBefore(Instant.now())) {
                log.warn("Token is expired");
                return Optional.empty();
            }

            return Optional.of(extractClaims(claims));

        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    private ValidatedClaims extractClaims(JWTClaimsSet claims) {
        String oid = getStringClaim(claims, "oid");
        String tid = getStringClaim(claims, "tid");
        String preferredUsername = getStringClaim(claims, "preferred_username");
        String name = getStringClaim(claims, "name");

        @SuppressWarnings("unchecked")
        List<String> roles = Optional.ofNullable(claims.getClaim("roles"))
                .filter(List.class::isInstance)
                .map(r -> (List<String>) r)
                .orElse(Collections.emptyList());

        @SuppressWarnings("unchecked")
        List<String> groups = Optional.ofNullable(claims.getClaim("groups"))
                .filter(List.class::isInstance)
                .map(g -> (List<String>) g)
                .orElse(Collections.emptyList());

        return new ValidatedClaims(oid, tid, preferredUsername, name, roles, groups);
    }

    private String getStringClaim(JWTClaimsSet claims, String claimName) {
        try {
            return claims.getStringClaim(claimName);
        } catch (Exception e) {
            return null;
        }
    }

    private JWKSet getJwkSet() throws Exception {
        jwksLock.readLock().lock();
        try {
            if (cachedJwkSet != null && Instant.now().isBefore(cacheExpiry)) {
                return cachedJwkSet;
            }
        } finally {
            jwksLock.readLock().unlock();
        }

        jwksLock.writeLock().lock();
        try {
            // Double-check after acquiring write lock
            if (cachedJwkSet != null && Instant.now().isBefore(cacheExpiry)) {
                return cachedJwkSet;
            }

            log.info("Refreshing JWKS from: {}", jwksUri);
            cachedJwkSet = JWKSet.load(URI.create(jwksUri).toURL());
            cacheExpiry = Instant.now().plus(cacheTtl);
            return cachedJwkSet;
        } finally {
            jwksLock.writeLock().unlock();
        }
    }

    /**
     * Validated claims extracted from Azure Entra ID JWT.
     */
    public record ValidatedClaims(
            String oid,
            String tenantId,
            String preferredUsername,
            String displayName,
            List<String> roles,
            List<String> groups
    ) {

        public Map<String, String> toHeaderMap(String orgId, List<String> internalRoles) {
            return Map.of(
                    "X-User-Id", oid != null ? oid : "",
                    "X-Org-Id", orgId != null ? orgId : "",
                    "X-Roles", String.join(",", internalRoles)
            );
        }
    }
}
