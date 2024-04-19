package com.myorg;

import software.amazon.awscdk.services.events.CronOptions;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.Schedule;
import software.amazon.awscdk.services.events.targets.LambdaFunction;
import software.amazon.awscdk.services.lambda.IFunction;
import software.constructs.Construct;

import java.util.List;

public class EventBridgeRule {
    private EventBridgeRule(){}

    public static Rule createRule(Construct scope, IFunction handler) {
        return Rule.Builder.create(scope, "getMatchesCron")
                .schedule(Schedule.cron(CronOptions.builder()
                        .hour("0")
                        .minute("30")
                        .build()))
                .targets(List.of(LambdaFunction.Builder.create(handler).build()))
                .build();
    }
}
