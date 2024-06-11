package com.mtjworldcup.cognito.service;

import com.mtjworldcup.cognito.exception.SignatureVerifierException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;

public class CognitoJwtVerifierService {

    private static final Logger log = LoggerFactory.getLogger(CognitoJwtVerifierService.class);

    public String checkUser(String token) throws SignatureVerifierException {
        try {
            JWT jwt = JWTParser.parse(token);
            if(!verifyToken(jwt))
                throw new SignatureVerifierException("Signature for token was not verified!");
            JWTClaimsSet jwtClaimsSet = jwt.getJWTClaimsSet();
            String subject = jwtClaimsSet.getSubject();
            return getUsername(subject);
        } catch (ParseException e) {
            throw new SignatureVerifierException("Parsing token to JWT failed. Cause: " + e.getMessage());
        }
    }

    String getUsername(String subject) {
        log.debug("Checking if user exists for subject: {}", subject);
        try(CognitoIdentityProviderClient client = CognitoIdentityProviderClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(Region.EU_CENTRAL_1)
                .build()) {
            String userPoolId = System.getenv("USER_POOL_ID");
            log.debug("User pool id: {}", userPoolId);
            AdminGetUserRequest request = AdminGetUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(subject)
                    .build();
            AdminGetUserResponse adminGetUserResponse = client.adminGetUser(request);
            Supplier<NoSuchElementException> userDoesNotExist = () -> new NoSuchElementException("User does not exist in user pool");
            boolean userEnabled = Optional.of(adminGetUserResponse)
                    .map(AdminGetUserResponse::enabled)
                    .orElseThrow(userDoesNotExist);
            if(!userEnabled) {
                throw new IllegalStateException("User is not enabled in user pool");
            }
            return Optional.of(adminGetUserResponse)
                    .map(AdminGetUserResponse::userAttributes)
                    .flatMap(attributes -> attributes.stream()
                            .filter(attribute -> attribute.name().equals("preferred_username"))
                            .findFirst()
                            .map(AttributeType::value))
                    .orElseThrow(userDoesNotExist);
        }
    }

    boolean verifyToken(JWT jwt) throws SignatureVerifierException{
        try {
            String jwksUrl = System.getenv("JWKS_URL");
            JWKSet publicKeys = JWKSet.load(new URL(jwksUrl));
            List<JWK> keys = publicKeys.getKeys();
            boolean signatureVerified = false;
            for (JWK key : keys) {
                if(key instanceof RSAKey rsaKey && jwt instanceof SignedJWT jws) {
                    signatureVerified = jws.verify(new RSASSAVerifier(rsaKey));
                    if(signatureVerified) {
                        break;
                    }
                }
            }
            return signatureVerified;
        } catch (IOException e) {
            throw new SignatureVerifierException("File could not be read. Cause: " + e.getMessage());
        } catch (ParseException e) {
            throw new SignatureVerifierException("File could not be parsed to a valid JWK set. Cause: " + e.getMessage());
        } catch (JOSEException e) {
            throw new SignatureVerifierException("RSA JWK extraction failed. Cause: " + e.getMessage());
        }
    }
}
