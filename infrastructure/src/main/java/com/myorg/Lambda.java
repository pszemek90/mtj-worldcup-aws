package com.myorg;

import software.amazon.awscdk.BundlingOptions;
import software.amazon.awscdk.DockerVolume;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.LayerVersion;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.assets.AssetOptions;
import software.constructs.Construct;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

import static software.amazon.awscdk.BundlingOutput.ARCHIVED;
import static software.amazon.awscdk.services.lambda.Runtime.JAVA_17;

public class Lambda {
    private Lambda() {}

    public static Function createLambda(Construct scope, String lambdaName, LayerVersion... layers) {
        List<String> packagingInstructions = createPackagingInstructions(lambdaName);
        BundlingOptions.Builder bundlingOptions = createBundlingOptions(packagingInstructions);
        return createFunction(scope, lambdaName, bundlingOptions, layers);
    }

    public static LayerVersion createLayer(Construct scope, String layerName) {
        return LayerVersion.Builder.create(scope, layerName)
                .code(Code.fromAsset(MessageFormat.format("../software/{0}/target/{0}-assembly.jar", layerName)))
                .compatibleRuntimes(List.of(JAVA_17))
                .build();
    }

    private static List<String> createPackagingInstructions(String lambdaName) {
        return List.of(
                "/bin/sh",
                "-c",
                MessageFormat.format("""
                cd {0} && mvn clean package -Pcdk \
                && cp /asset-input/{0}/target-cdk/{0}.jar /asset-output/""", lambdaName)
        );
    }

    private static BundlingOptions.Builder createBundlingOptions(List<String> packagingInstructions) {
        return BundlingOptions.builder()
                .command(packagingInstructions)
                .image(JAVA_17.getBundlingImage())
                .volumes(List.of(
                        DockerVolume.builder()
                                .hostPath(System.getProperty("user.home") + "/.m2/")
                                .containerPath("/root/.m2/")
                                .build()))
                .user("root")
                .outputType(ARCHIVED);
    }

    private static Function createFunction(Construct scope, String lambdaName, BundlingOptions.Builder bundlingOptions, LayerVersion... layers) {
        return new Function(scope, lambdaName, FunctionProps.builder()
                .runtime(JAVA_17)
                .code(Code.fromAsset("../software/",
                        AssetOptions.builder()
                                .bundling(bundlingOptions.build())
                                .build()))
                .handler("com.mtjworldcup.Handler")
                .memorySize(1024)
                .timeout(Duration.seconds(30))
                .logRetention(RetentionDays.ONE_WEEK)
                .layers(Arrays.asList(layers))
                .build());
    }
}
