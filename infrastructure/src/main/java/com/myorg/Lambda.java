package com.myorg;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.LayerVersion;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

import static software.amazon.awscdk.services.lambda.Runtime.JAVA_17;

public class Lambda {
    private Lambda() {}

    public static Function createLambda(Construct scope, String lambdaName, String packageName,  LayerVersion... layers) {
        return new Function(scope, lambdaName, FunctionProps.builder()
                .runtime(JAVA_17)
                .code(Code.fromAsset(MessageFormat.format("../software/{0}/target/{0}.jar", lambdaName)))
                .handler(MessageFormat.format("com.mtjworldcup.{0}.Handler", packageName))
                .memorySize(1024)
                .timeout(Duration.seconds(30))
                .logRetention(RetentionDays.ONE_WEEK)
                .layers(Arrays.asList(layers))
                .build());
    }

    public static LayerVersion createLayer(Construct scope, String layerName) {
        return LayerVersion.Builder.create(scope, layerName)
                .code(Code.fromAsset(MessageFormat.format("../software/{0}/target/{0}-assembly.jar", layerName)))
                .compatibleRuntimes(List.of(JAVA_17))
                .build();
    }
}
