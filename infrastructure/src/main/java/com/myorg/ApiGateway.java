package com.myorg;

import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.cognito.IUserPool;
import software.constructs.Construct;

import java.util.List;

public class ApiGateway {
    private ApiGateway(){}

    public static RestApi createRestApi(Construct scope, IUserPool userPool) {
        return RestApi.Builder.create(scope, "worldcup-api")
                .deployOptions(StageOptions.builder()
                        .throttlingBurstLimit(1)
                        .throttlingRateLimit(1)
                        .loggingLevel(MethodLoggingLevel.ERROR)
                        .build())
                .cloudWatchRole(true)
                .defaultCorsPreflightOptions(CorsOptions.builder().allowOrigins(List.of("http://localhost:5173")).build())
                .defaultMethodOptions(MethodOptions.builder()
                        .authorizationType(AuthorizationType.COGNITO)
                        .authorizer(CognitoUserPoolsAuthorizer.Builder.create(scope, "worldcup-user-pool-authorizer")
                                .cognitoUserPools(List.of(userPool))
                                .build())
                        .build())
                .build();
    }
}
