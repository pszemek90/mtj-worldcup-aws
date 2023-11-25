package com.mtjworldcup.service;

import com.mtjworldcup.exception.SignatureVerifierException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.List;
import java.util.NoSuchElementException;

public class CognitoJwtVerifierService {

    public String getSubject(String token) throws ParseException, IOException, JOSEException, SignatureVerifierException {
        JWT jwt = JWTParser.parse(token);
        if(!verifyToken(jwt))
            throw new SignatureVerifierException("Signature for token was not verified!");
        JWTClaimsSet jwtClaimsSet = jwt.getJWTClaimsSet();
        String subject = jwtClaimsSet.getSubject();
        if(!userExists(subject)){
            throw new NoSuchElementException("User does not exist in user pool");
        }
        return subject;
    }

    boolean userExists(String subject) {
        try(CognitoIdentityProviderClient client = CognitoIdentityProviderClient.builder()
                .region(Region.EU_CENTRAL_1)
                .build()) {
            String userPoolId = System.getenv("USER_POOL_ID");
            AdminGetUserRequest request = AdminGetUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(subject)
                    .build();
            client.adminGetUser(request);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    boolean verifyToken(JWT jwt) throws JOSEException, IOException, ParseException {
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
    }
}
