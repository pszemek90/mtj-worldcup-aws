package com.myorg;

import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.Schedule;
import software.amazon.awscdk.services.events.targets.LambdaFunction;
import software.amazon.awscdk.services.lambda.IFunction;
import software.constructs.Construct;

import java.util.List;

public class EventBridgeRule {
    private EventBridgeRule(){}

    public static Rule createRule(Construct scope, IFunction handler, Schedule schedule, String name) {
        return Rule.Builder.create(scope, name)
                .schedule(schedule)
                .targets(List.of(LambdaFunction.Builder.create(handler).build()))
                .build();
    }
}
