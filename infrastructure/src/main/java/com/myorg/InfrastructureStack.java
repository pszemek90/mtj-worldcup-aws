package com.myorg;

import java.util.List;
import java.util.Map;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.MethodOptions;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.cognito.IUserPool;
import software.amazon.awscdk.services.cognito.UserPool;
import software.amazon.awscdk.services.dynamodb.TableV2;
import software.amazon.awscdk.services.events.CronOptions;
import software.amazon.awscdk.services.events.Schedule;
import software.amazon.awscdk.services.lambda.FilterCriteria;
import software.amazon.awscdk.services.lambda.FilterRule;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.LayerVersion;
import software.amazon.awscdk.services.lambda.StartingPosition;
import software.amazon.awscdk.services.lambda.eventsources.DynamoEventSource;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;

public class InfrastructureStack extends Stack {
  public InfrastructureStack(final Construct scope, final String id) {
    this(scope, id, null);
  }

  public InfrastructureStack(final Construct scope, final String id, final StackProps props) {
    super(scope, id, props);

    LayerVersion dynamoDbLayer = Lambda.createLayer(this, "dynamo-db-layer");
    LayerVersion worldcupCommonLayer = Lambda.createLayer(this, "worldcup-common-layer");
    LayerVersion cognitoLayer = Lambda.createLayer(this, "cognito-layer");

    Function getMatchesFromApi =
        Lambda.createLambda(
            this, "getMatchesFromApi", "getfromapi", dynamoDbLayer, worldcupCommonLayer);

    Function getMatchesByDate =
        Lambda.createLambda(
            this, "getMatchesByDate", "getbydate", dynamoDbLayer, worldcupCommonLayer);

    Function postTypes =
        Lambda.createLambda(
            this, "postTypes", "posttypes", dynamoDbLayer, worldcupCommonLayer, cognitoLayer);

    Function getResults =
        Lambda.createLambda(this, "getResults", "getresults", dynamoDbLayer, worldcupCommonLayer);

    Function getMyTypings =
        Lambda.createLambda(
            this, "getMyTypings", "getmytypings", dynamoDbLayer, worldcupCommonLayer, cognitoLayer);

    Function getAllTypings =
        Lambda.createLambda(
            this, "getAllTypings", "getalltypings", dynamoDbLayer, worldcupCommonLayer);

    Function getOverallPool =
        Lambda.createLambda(
            this, "getOverallPool", "getoverallpool", dynamoDbLayer, worldcupCommonLayer);

    Function getUserProfile =
        Lambda.createLambda(
            this,
            "getUserProfile",
            "getuserprofile",
            dynamoDbLayer,
            worldcupCommonLayer,
            cognitoLayer);

    Function getCurrentStateFromApi =
        Lambda.createLambda(
            this,
            "getCurrentStateFromApi",
            "getcurrentstatefromapi",
            dynamoDbLayer,
            worldcupCommonLayer);

    Function dividePool =
        Lambda.createLambda(this, "dividePool", "dividepool", dynamoDbLayer, worldcupCommonLayer);

    Function handleFinishedMatch =
        Lambda.createLambda(
            this, "handleFinishedMatch", "handlefinishedmatch", dynamoDbLayer, worldcupCommonLayer);

    TableV2 matchesTable = DynamoDb.createTable(this);

    matchesTable.grantReadWriteData(getMatchesFromApi);
    matchesTable.grantReadData(getMatchesByDate);
    matchesTable.grantReadWriteData(postTypes);
    matchesTable.grantReadData(getResults);
    matchesTable.grantReadData(getMyTypings);
    matchesTable.grantReadData(getAllTypings);
    matchesTable.grantReadData(getOverallPool);
    matchesTable.grantReadData(getUserProfile);
    matchesTable.grantReadWriteData(getCurrentStateFromApi);
    matchesTable.grantReadWriteData(dividePool);
    matchesTable.grantReadWriteData(handleFinishedMatch);
    matchesTable.grantStreamRead(handleFinishedMatch);

    String userPoolId = "USER_POOL_ID";
    String userPoolIdFromSsm = StringParameter.valueForStringParameter(this, userPoolId);

    IUserPool worldcupUserPool =
        UserPool.fromUserPoolId(this, "worldcup-user-pool", userPoolIdFromSsm);
    String adminGetUser = "cognito-idp:AdminGetUser";
    worldcupUserPool.grant(postTypes, adminGetUser);
    worldcupUserPool.grant(getMyTypings, adminGetUser);
    worldcupUserPool.grant(getUserProfile, adminGetUser);

    String matchesTableName = "MATCHES_TABLE_NAME";
    String jwksUrl = "JWKS_URL";
    String rapidApiKey = "RAPID_API_KEY";
    String rapidApiHost = "RAPID_API_HOST";
    String baseUrl = "BASE_API_URL";
    String rapidApiKeyFromSsm = StringParameter.valueForStringParameter(this, rapidApiKey);
    String rapiApiHostFromSsm = StringParameter.valueForStringParameter(this, rapidApiHost);
    String jwksUrlFromSsm = StringParameter.valueForStringParameter(this, jwksUrl);
    String baseMatchApiUrlFromSsm = StringParameter.valueForStringParameter(this, baseUrl);

    getMatchesFromApi.addEnvironment(matchesTableName, matchesTable.getTableName());
    getMatchesFromApi.addEnvironment(rapidApiKey, rapidApiKeyFromSsm);
    getMatchesFromApi.addEnvironment(rapidApiHost, rapiApiHostFromSsm);
    getMatchesFromApi.addEnvironment(baseUrl, baseMatchApiUrlFromSsm);

    getMatchesByDate.addEnvironment(matchesTableName, matchesTable.getTableName());

    postTypes.addEnvironment(matchesTableName, matchesTable.getTableName());
    postTypes.addEnvironment(jwksUrl, jwksUrlFromSsm);
    postTypes.addEnvironment(userPoolId, userPoolIdFromSsm);

    getResults.addEnvironment(matchesTableName, matchesTable.getTableName());

    getMyTypings.addEnvironment(matchesTableName, matchesTable.getTableName());
    getMyTypings.addEnvironment(jwksUrl, jwksUrlFromSsm);
    getMyTypings.addEnvironment(userPoolId, userPoolIdFromSsm);

    getAllTypings.addEnvironment(matchesTableName, matchesTable.getTableName());

    getOverallPool.addEnvironment(matchesTableName, matchesTable.getTableName());

    getUserProfile.addEnvironment(userPoolId, userPoolIdFromSsm);
    getUserProfile.addEnvironment(jwksUrl, jwksUrlFromSsm);
    getUserProfile.addEnvironment(matchesTableName, matchesTable.getTableName());

    getCurrentStateFromApi.addEnvironment(matchesTableName, matchesTable.getTableName());
    getCurrentStateFromApi.addEnvironment(rapidApiKey, rapidApiKeyFromSsm);
    getCurrentStateFromApi.addEnvironment(rapidApiHost, rapiApiHostFromSsm);
    getCurrentStateFromApi.addEnvironment(baseUrl, baseMatchApiUrlFromSsm);

    dividePool.addEnvironment(matchesTableName, matchesTable.getTableName());

    handleFinishedMatch.addEnvironment(matchesTableName, matchesTable.getTableName());

    handleFinishedMatch.addEventSource(
        DynamoEventSource.Builder.create(matchesTable)
            .startingPosition(StartingPosition.TRIM_HORIZON)
            .filters(
                List.of(
                    FilterCriteria.filter(
                        Map.of(
                            "dynamodb",
                            Map.of(
                                "NewImage",
                                Map.of(
                                    "match_status",
                                    Map.of("S", FilterRule.isEqual("FINISHED"))))))))
            .batchSize(1)
            .build());

    Schedule onceADay = Schedule.cron(CronOptions.builder().hour("0").minute("30").build());
    EventBridgeRule.createRule(this, getMatchesFromApi, onceADay, "getMatchesCron");

    Schedule everyHour = Schedule.cron(CronOptions.builder().minute("15").build());
    EventBridgeRule.createRule(this, getCurrentStateFromApi, everyHour, "getCurrentStateCron");

    onceADay = Schedule.cron(CronOptions.builder().hour("0").minute("45").build());
    EventBridgeRule.createRule(this, dividePool, onceADay, "dividePoolCron");

    RestApi api = ApiGateway.createRestApi(this, worldcupUserPool);
    api.getRoot()
        .addResource("api")
        .addResource("results")
        .addMethod("GET", LambdaIntegration.Builder.create(getResults).build())
        .getResource()
        .getParentResource()
        .addResource("typings")
        .addMethod("GET", LambdaIntegration.Builder.create(getMyTypings).build())
        .getResource()
        .getParentResource()
        .addResource("all-typings")
        .addMethod("GET", LambdaIntegration.Builder.create(getAllTypings).build())
        .getResource()
        .getParentResource()
        .addResource("overall-pool")
        .addMethod("GET", LambdaIntegration.Builder.create(getOverallPool).build())
        .getResource()
        .getParentResource()
        .addResource("user-profile")
        .addMethod("GET", LambdaIntegration.Builder.create(getUserProfile).build())
        .getResource()
        .getParentResource()
        .addResource("matches")
        .addResource("{date}")
        .addMethod(
            "GET",
            LambdaIntegration.Builder.create(getMatchesByDate).build(),
            MethodOptions.builder()
                .requestParameters(Map.of("method.request.path.date", true))
                .build())
        .getResource()
        .addMethod(
            "POST",
            LambdaIntegration.Builder.create(postTypes).build(),
            MethodOptions.builder()
                .requestParameters(Map.of("method.request.path.date", true))
                .build());
  }
}
