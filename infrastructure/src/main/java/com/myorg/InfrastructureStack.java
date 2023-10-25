package com.myorg;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.dynamodb.*;
import software.amazon.awscdk.services.events.CronOptions;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.Schedule;
import software.amazon.awscdk.services.events.targets.LambdaFunction;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
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

        BundlingOptions.Builder builderOptions = BundlingOptions.builder()
                .command(getMatchesFromApiPackagingInstructions)
                .image(JAVA_17.getBundlingImage())
                .volumes(List.of(
                        DockerVolume.builder()
                                .hostPath(System.getProperty("user.home") + "/.m2/")
                                .containerPath("/root/.m2/")
                                .build()
                ))
                .user("root")
                .outputType(ARCHIVED);

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
                .globalSecondaryIndexes(List.of(
                        GlobalSecondaryIndexPropsV2.builder()
                                .readCapacity(Capacity.fixed(1))
                                .writeCapacity(Capacity.autoscaled(
                                        AutoscaledCapacityOptions.builder()
                                                .maxCapacity(1)
                                                .build()))
                                .indexName("start_time")
                                .nonKeyAttributes(List.of(
                                        "home_team",
                                        "away_team"))
                                .partitionKey(Attribute.builder()
                                        .name("start_time")
                                        .type(AttributeType.STRING)
                                        .build())
                                .projectionType(ProjectionType.INCLUDE)
                                .build()))
                .build();

        matchesTable.grantReadWriteData(getMatchesFromApi);

        getMatchesFromApi.addEnvironment("MATCHES_TABLE_NAME", matchesTable.getTableName());
        getMatchesFromApi.addEnvironment("RAPID_API_KEY", StringParameter.valueForStringParameter(this, "RAPID_API_KEY"));

        Rule getMatchesFromApiRule = Rule.Builder.create(this, "getMatchesCron")
                .schedule(Schedule.cron(CronOptions.builder()
                        .weekDay("SUNDAY")
                        .hour("6")
                        .minute("0")
                        .build()))
                .targets(List.of(LambdaFunction.Builder.create(getMatchesFromApi).build()))
                .build();

    }
}
