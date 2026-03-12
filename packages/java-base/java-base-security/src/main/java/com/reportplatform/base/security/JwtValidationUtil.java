package com.reportplatform.base.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.text.ParseException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JWT validation utilities for Azure Entra ID (formerly Azure AD) tokens.
 * Performs JWKS-based signature validation and claims extraction.
 */
public final class JwtValidationUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtValidationUtil.class);

    private static final String AZURE_JWKS_URL_TEMPLATE =
            "https://login.microsoftonline.com/%s/discovery/v2.0/keys";

    private static final Duration JWKS_CACHE_LIFESPAN = Duration.ofHours(24);
    private static final Duration JWKS_CACHE_REFRESH = Duration.ofMinutes(5);

    private final ConfigurableJWTProcessor<SecurityContext> jwtProcessor;
    private final String expectedAudience;
    private final String expectedIssuer;

    /**
     * Creates a new JwtValidationUtil configured for the given Azure Entra ID tenant.
     *
     * @param tenantId         Azure Entra ID tenant ID
     * @param expectedAudience expected audience claim (application/client ID)
     */
    public JwtValidationUtil(String tenantId, String expectedAudience) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(expectedAudience, "expectedAudience must not be null");

        this.expectedAudience = expectedAudience;
        this.expectedIssuer = "https://login.microsoftonline.com/" + tenantId + "/v2.0";

        try {
            var jwksUrl = URI.create(String.format(AZURE_JWKS_URL_TEMPLATE, tenantId)).toURL();

            JWKSource<SecurityContext> jwkSource = JWKSourceBuilder
                    .create(jwksUrl)
                    .cache(JWKS_CACHE_LIFESPAN.toMillis(), JWKS_CACHE_REFRESH.toMillis())
                    .rateLimited(false)
                    .build();

            JWSKeySelector<SecurityContext> keySelector =
                    new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource);

            this.jwtProcessor = new DefaultJWTProcessor<>();
            this.jwtProcessor.setJWSKeySelector(keySelector);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid tenant ID produced malformed JWKS URL", e);
        }
    }

    /**
     * Validates a JWT token and returns the claims set.
     *
     * @param token the raw JWT token string (without "Bearer " prefix)
     * @return validated claims set
     * @throws JwtValidationException if the token is invalid
     */
    public JWTClaimsSet validateToken(String token) {
        Objects.requireNonNull(token, "token must not be null");

        try {
            JWTClaimsSet claims = jwtProcessor.process(token, null);

            validateIssuer(claims);
            validateAudience(claims);

            return claims;
        } catch (ParseException e) {
            throw new JwtValidationException("Failed to parse JWT token", e);
        } catch (BadJOSEException e) {
            throw new JwtValidationException("JWT signature validation failed", e);
        } catch (JOSEException e) {
            throw new JwtValidationException("JWT processing error", e);
        }
    }

    /**
     * Extracts the object ID (oid) claim - the unique user identifier in Azure Entra ID.
     */
    public static String extractObjectId(JWTClaimsSet claims) {
        return getStringClaim(claims, "oid");
    }

    /**
     * Extracts the tenant ID (tid) claim.
     */
    public static String extractTenantId(JWTClaimsSet claims) {
        return getStringClaim(claims, "tid");
    }

    /**
     * Extracts the roles claim from the JWT token.
     * Returns app roles assigned to the user for this application.
     */
    @SuppressWarnings("unchecked")
    public static List<String> extractRoles(JWTClaimsSet claims) {
        try {
            List<String> roles = claims.getStringListClaim("roles");
            return roles != null ? Collections.unmodifiableList(roles) : Collections.emptyList();
        } catch (ParseException e) {
            log.warn("Failed to parse roles claim from JWT", e);
            return Collections.emptyList();
        }
    }

    /**
     * Extracts the groups claim from the JWT token.
     * Returns security group IDs the user belongs to.
     */
    @SuppressWarnings("unchecked")
    public static List<String> extractGroups(JWTClaimsSet claims) {
        try {
            List<String> groups = claims.getStringListClaim("groups");
            return groups != null ? Collections.unmodifiableList(groups) : Collections.emptyList();
        } catch (ParseException e) {
            log.warn("Failed to parse groups claim from JWT", e);
            return Collections.emptyList();
        }
    }

    /**
     * Extracts the preferred username (upn or preferred_username) from the JWT token.
     */
    public static Optional<String> extractPreferredUsername(JWTClaimsSet claims) {
        String upn = getStringClaim(claims, "upn");
        if (upn != null) {
            return Optional.of(upn);
        }
        return Optional.ofNullable(getStringClaim(claims, "preferred_username"));
    }

    private void validateIssuer(JWTClaimsSet claims) {
        String issuer = claims.getIssuer();
        if (!expectedIssuer.equals(issuer)) {
            throw new JwtValidationException(
                    "Invalid issuer. Expected: " + expectedIssuer + ", got: " + issuer);
        }
    }

    private void validateAudience(JWTClaimsSet claims) {
        List<String> audience = claims.getAudience();
        if (audience == null || !audience.contains(expectedAudience)) {
            throw new JwtValidationException(
                    "Invalid audience. Expected: " + expectedAudience + ", got: " + audience);
        }
    }

    private static String getStringClaim(JWTClaimsSet claims, String claimName) {
        try {
            return claims.getStringClaim(claimName);
        } catch (ParseException e) {
            log.warn("Failed to parse claim '{}' from JWT", claimName, e);
            return null;
        }
    }

    /**
     * Exception thrown when JWT validation fails.
     */
    public static class JwtValidationException extends RuntimeException {
        public JwtValidationException(String message) {
            super(message);
        }

        public JwtValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
