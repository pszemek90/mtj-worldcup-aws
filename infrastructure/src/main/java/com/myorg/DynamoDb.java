package com.myorg;

import software.amazon.awscdk.services.dynamodb.*;
import software.constructs.Construct;

import java.util.List;

public class DynamoDb {

    private DynamoDb() {}

    public static TableV2 createTable(Construct scope) {
        return TableV2.Builder.create(scope, "matches")
                .partitionKey(Attribute.builder()
                        .name("primary_id")
                        .type(AttributeType.STRING)
                        .build())
                .sortKey(Attribute.builder()
                        .name("secondary_id")
                        .type(AttributeType.STRING)
                        .build())
                .globalSecondaryIndexes(List.of(
                        GlobalSecondaryIndexPropsV2.builder()
                                .partitionKey(Attribute.builder()
                                        .name("secondary_id")
                                        .type(AttributeType.STRING)
                                        .build())
                                .sortKey(Attribute.builder()
                                        .name("primary_key")
                                        .type(AttributeType.STRING)
                                        .build())
                                .indexName("getBySecondaryId")
                                .readCapacity(Capacity.fixed(1))
                                .writeCapacity(Capacity.autoscaled(
                                        AutoscaledCapacityOptions.builder()
                                                .maxCapacity(1)
                                                .build()))
                                .build(),
                        GlobalSecondaryIndexPropsV2.builder()
                                .partitionKey(Attribute.builder()
                                        .name("date")
                                        .type(AttributeType.STRING)
                                        .build())
                                .indexName("getByDate")
                                .projectionType(ProjectionType.INCLUDE)
                                .nonKeyAttributes(List.of("home_team", "away_team", "start_time"))
                                .readCapacity(Capacity.fixed(1))
                                .writeCapacity(Capacity.autoscaled(
                                        AutoscaledCapacityOptions.builder()
                                                .maxCapacity(1)
                                                .build()))
                                .build()))
                .billing(Billing.provisioned(ThroughputProps.builder()
                        .readCapacity(Capacity.fixed(1))
                        .writeCapacity(Capacity.autoscaled(
                                AutoscaledCapacityOptions.builder()
                                        .maxCapacity(1)
                                        .build()))
                        .build()))
                .build();
    }
}
