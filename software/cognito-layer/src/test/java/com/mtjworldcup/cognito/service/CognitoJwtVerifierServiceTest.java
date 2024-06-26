package com.mtjworldcup.cognito.service;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@ExtendWith(SystemStubsExtension.class)
class CognitoJwtVerifierServiceTest {

    private final CognitoJwtVerifierService service = new CognitoJwtVerifierService();
    @SystemStub
    private EnvironmentVariables environmentVariables;

    @Test
    void shouldGetUser_WhenCorrectTokenPassed() throws Exception{
        String token = Files.readString(Path.of("src/test/resources/jwt.txt"));
        CognitoJwtVerifierService spy = spy(service);
        doReturn(true).when(spy).verifyToken(any());
        doReturn("user-1").when(spy).getUsername(any());
        String username = spy.checkUser(token);
        String expectedUsername = "user-1";
        assertEquals(expectedUsername, username);
    }

    @Test
    void shouldPassTokenVerification_WhenCorrectTokenPassed() throws Exception{
        String token = Files.readString(Path.of("src/test/resources/jwt.txt"));
        CognitoJwtVerifierService spy = spy(service);
        doReturn("user-1").when(spy).getUsername(any());
        environmentVariables.set("JWKS_URL", "file:src/test/resources/jwks.json");
        JWT jwt = JWTParser.parse(token);
        assertTrue(service.verifyToken(jwt));
    }

}