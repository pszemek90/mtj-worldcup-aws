package com.myorg;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.assets.AssetOptions;
import software.constructs.Construct;

import java.util.List;

import static software.amazon.awscdk.BundlingOutput.ARCHIVED;
import static software.amazon.awscdk.services.lambda.Runtime.JAVA_17;
// import software.amazon.awscdk.Duration;
// import software.amazon.awscdk.services.sqs.Queue;

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
                cd getMatchesFromApi && mvn clean install \
                && cp /asset-input/getMatchesFromApi/target/getMatchesFromApi.jar /asset-output/
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
    }
}
