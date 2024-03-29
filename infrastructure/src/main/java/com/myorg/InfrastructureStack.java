package com.myorg;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.MethodOptions;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.dynamodb.TableV2;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.LayerVersion;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;

import java.util.Map;

public class InfrastructureStack extends Stack {
    public InfrastructureStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public InfrastructureStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        LayerVersion dynamoDbLayer = Lambda.createLayer(this, "dynamo-db-layer");
        LayerVersion worldcupCommonLayer = Lambda.createLayer(this, "worldcup-common-layer");
        LayerVersion cognitoLayer = Lambda.createLayer(this, "cognito-layer");

        Function getMatchesFromApi = Lambda.createLambda(this, "getMatchesFromApi",
                dynamoDbLayer, worldcupCommonLayer);

        Function getMatchesByDate = Lambda.createLambda(this, "getMatchesByDate",
                dynamoDbLayer, worldcupCommonLayer);

        Function postTypes = Lambda.createLambda(this, "postTypes", dynamoDbLayer, worldcupCommonLayer, cognitoLayer);

        Function getResults = Lambda.createLambda(this, "getResults", dynamoDbLayer, worldcupCommonLayer);

        TableV2 matchesTable = DynamoDb.createTable(this);

        matchesTable.grantReadWriteData(getMatchesFromApi);
        matchesTable.grantReadData(getMatchesByDate);
        matchesTable.grantReadWriteData(postTypes);
        matchesTable.grantReadData(getResults);

        String matchesTableName = "MATCHES_TABLE_NAME";
        getMatchesFromApi.addEnvironment(matchesTableName, matchesTable.getTableName());
        getMatchesByDate.addEnvironment(matchesTableName, matchesTable.getTableName());
        postTypes.addEnvironment(matchesTableName, matchesTable.getTableName());
        getResults.addEnvironment(matchesTableName, matchesTable.getTableName());
        getMatchesFromApi.addEnvironment("RAPID_API_KEY", StringParameter.valueForStringParameter(this, "RAPID_API_KEY"));

        Rule getMatchesFromApiRule = EventBridgeRule.createRule(this, getMatchesFromApi);

        RestApi api = ApiGateway.createRestApi(this);
        api.getRoot()
                .addResource("api")
                .addResource("results")
                .addMethod("GET", LambdaIntegration.Builder.create(getResults).build())
                .getResource()
                .getParentResource()
                .addResource("matches")
                .addResource("{date}")
                .addMethod("GET", LambdaIntegration.Builder.create(getMatchesByDate).build(),
                        MethodOptions.builder()
                                .requestParameters(Map.of("method.request.path.date", true))
                                .build())
                .getResource()
                .addMethod("POST", LambdaIntegration.Builder.create(postTypes).build(),
                        MethodOptions.builder()
                                .requestParameters(Map.of("method.request.path.date", true))
                                .build());
    }
}
