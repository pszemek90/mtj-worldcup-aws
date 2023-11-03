package com.myorg;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.dynamodb.*;
import software.amazon.awscdk.services.events.CronOptions;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.Schedule;
import software.amazon.awscdk.services.events.targets.LambdaFunction;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.LayerVersion;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.assets.AssetOptions;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;

import java.util.List;

import static software.amazon.awscdk.BundlingOutput.ARCHIVED;
import static software.amazon.awscdk.services.lambda.Runtime.JAVA_17;

public class InfrastructureStack extends Stack {
    public InfrastructureStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public InfrastructureStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        List<String> getMatchesFromApiPackagingInstructions = List.of(
                "/bin/sh",
                "-c",
                """
                cd getMatchesFromApi && mvn clean package -Pcdk \
                && cp /asset-input/getMatchesFromApi/target-cdk/getMatchesFromApi.jar /asset-output/
                """
        );

        List<String> getMatchesByDatePackagingInstructions = List.of(
                "/bin/sh",
                "-c",
                """
                cd getMatchesByDate && mvn clean package -Pcdk \
                && cp /asset-input/getMatchesByDate/target-cdk/getMatchesByDate.jar /asset-output/
                """
        );

        BundlingOptions.Builder builderOptions = BundlingOptions.builder()
                .command(getMatchesFromApiPackagingInstructions)
                .image(JAVA_17.getBundlingImage())
                .volumes(List.of(
                        DockerVolume.builder()
                                .hostPath(System.getProperty("user.home") + "/.m2/")
                                .containerPath("/root/.m2/")
                                .build()))
                .user("root")
                .outputType(ARCHIVED);

        LayerVersion dynamoDbLayer = LayerVersion.Builder.create(this, "dynamo-db-layer")
                .code(Code.fromAsset("../software/dynamo-db-layer/target/dynamo-db-layer-assembly.jar"))
                .compatibleRuntimes(List.of(JAVA_17))
                .build();

        LayerVersion worldcupCommonLayer = LayerVersion.Builder.create(this, "worldcup-common-layer")
                .code(Code.fromAsset("../software/worldcup-common-layer/target/worldcup-common-layer-assembly.jar"))
                .compatibleRuntimes(List.of(JAVA_17))
                .build();

        Function getMatchesFromApi = new Function(this, "getMatchesFromApi", FunctionProps.builder()
                .runtime(JAVA_17)
                .code(Code.fromAsset("../software/",
                        AssetOptions.builder()
                                .bundling(builderOptions
                                        .command(getMatchesFromApiPackagingInstructions)
                                        .build())
                                .build()))
                .handler("com.mtjworldcup.MatchHandler")
                .memorySize(1024)
                .timeout(Duration.seconds(30))
                .logRetention(RetentionDays.ONE_WEEK)
                .layers(List.of(dynamoDbLayer, worldcupCommonLayer))
                .build());

        Function getMatchesByDate = new Function(this, "getMatchesByDate", FunctionProps.builder()
                .runtime(JAVA_17)
                .code(Code.fromAsset("../software/",
                        AssetOptions.builder()
                                .bundling(builderOptions
                                        .command(getMatchesByDatePackagingInstructions)
                                        .build())
                                .build()))
                .handler("com.mtjworldcup.Handler")
                .memorySize(1024)
                .timeout(Duration.seconds(30))
                .logRetention(RetentionDays.ONE_WEEK)
                .layers(List.of(dynamoDbLayer, worldcupCommonLayer))
                .build());

        TableV2 matchesTable = TableV2.Builder.create(this, "matches")
                .partitionKey(Attribute.builder()
                        .name("match_id")
                        .type(AttributeType.NUMBER)
                        .build())
                .billing(Billing.provisioned(ThroughputProps.builder()
                        .readCapacity(Capacity.fixed(1))
                        .writeCapacity(Capacity.autoscaled(
                                AutoscaledCapacityOptions.builder()
                                        .maxCapacity(1)
                                        .build()))
                        .build()))
                .build();

        matchesTable.grantReadWriteData(getMatchesFromApi);
        matchesTable.grantReadData(getMatchesByDate);

        getMatchesFromApi.addEnvironment("MATCHES_TABLE_NAME", matchesTable.getTableName());
        getMatchesByDate.addEnvironment("MATCHES_TABLE_NAME", matchesTable.getTableName());
        getMatchesFromApi.addEnvironment("RAPID_API_KEY", StringParameter.valueForStringParameter(this, "RAPID_API_KEY"));

        Rule getMatchesFromApiRule = Rule.Builder.create(this, "getMatchesCron")
                .schedule(Schedule.cron(CronOptions.builder()
                        .weekDay("SUNDAY")
                        .hour("6")
                        .minute("0")
                        .build()))
                .targets(List.of(LambdaFunction.Builder.create(getMatchesFromApi).build()))
                .build();

        RestApi api = RestApi.Builder.create(this, "worldcup-api")
                .defaultMethodOptions(MethodOptions.builder()
                        .methodResponses(List.of(MethodResponse.builder()
                                .statusCode("200")
                                .build()))
                        .build())
                .build();
        api.getRoot()
                .addResource("matches")
                .addMethod("POST", LambdaIntegration.Builder.create(getMatchesByDate)
                .integrationResponses(List.of(IntegrationResponse.builder().statusCode("200").build()))
                .proxy(false)
                .build());

    }
}
