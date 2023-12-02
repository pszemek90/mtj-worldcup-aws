package com.mtjworldcup.dao;

import com.mtjworldcup.model.Match;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteResult;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.stream.Stream;

import static java.time.Month.OCTOBER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SystemStubsExtension.class)
class MatchesDaoTest {

    private final DynamoDbClient mockDynamoDbClient = mock(DynamoDbClient.class);
    private final DynamoDbEnhancedClient mockEnhancedClient = mock(DynamoDbEnhancedClient.class);
    @SystemStub
    private EnvironmentVariables environmentVariables;

    @Test
    void shouldReturnOneMatch_WhenOneMatchInDb() {
        //given
        environmentVariables.set("MATCHES_TABLE_NAME", "matches");
        MatchesDao matchesDao = new MatchesDao(mockDynamoDbClient, mockEnhancedClient);
        LocalDate matchDate = LocalDate.of(2023, OCTOBER, 29);
        Stream<Match> matchStream = prepareMatches(1);
        MatchesDao matchesDaoSpy = spy(matchesDao);
        doReturn(matchStream).when(matchesDaoSpy).getItemsFromDb(any(), any());
        //when
        List<Match> matchesFromDatabase = matchesDaoSpy.getByDate(matchDate);
        //then
        assertEquals(1, matchesFromDatabase.size());
    }

    @Test
    void shouldReturnTwoMatches_WhenTwoMatchesInDb() {
        //given
        environmentVariables.set("MATCHES_TABLE_NAME", "matches");
        MatchesDao matchesDao = new MatchesDao(mockDynamoDbClient, mockEnhancedClient);
        LocalDate matchDate = LocalDate.of(2023, OCTOBER, 29);
        Stream<Match> matchStream = prepareMatches(2);
        MatchesDao matchesDaoSpy = spy(matchesDao);
        doReturn(matchStream).when(matchesDaoSpy).getItemsFromDb(any(), any());
        //when
        List<Match> matchesFromDatabase = matchesDaoSpy.getByDate(matchDate);
        //then
        assertEquals(2, matchesFromDatabase.size());
    }

    private Stream<Match> prepareMatches(int numberOfMatches) {
        ArrayList<Match> matches = new ArrayList<>();
        for (int i = 0; i < numberOfMatches; i++) {
            Match match = new Match();
            match.setAwayScore(i % 4);
            match.setHomeScore(i % 2);
            match.setPrimaryId("match-" + i);
            match.setSecondaryId("match-" + i);
            match.setStartTime(LocalTime.of( i % 23, i % 59));
            match.setAwayTeam("team" + (i + 1));
            match.setHomeTeam("team" + (i + 2));
            matches.add(match);
        }
        return matches.stream();
    }
}

@ExtendWith(SystemStubsExtension.class)
@Testcontainers
class MatchDaoIntegrationTest{

    private static final Logger log = LoggerFactory.getLogger(MatchesDaoTest.class);
    @SystemStub
    private EnvironmentVariables environmentVariables;

    @Container
    private static final LocalStackContainer localStack =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:2.0.0"))
                    .withServices(LocalStackContainer.Service.DYNAMODB);
    private static DynamoDbClient localstackDynamoClient;
    private static DynamoDbEnhancedClient localstackEnhancedClient;
    private static DynamoDbTable<Match> matches;
    private static MatchesDao matchesDao;

    @BeforeAll
    static void setUp() {
        localstackDynamoClient = DynamoDbClient.builder()
                .endpointOverride(localStack.getEndpoint())
                .credentialsProvider(StaticCredentialsProvider
                        .create(AwsBasicCredentials
                                .create(localStack.getAccessKey(), localStack.getSecretKey())))
                .region(Region.of(localStack.getRegion()))
                .build();
        localstackEnhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(localstackDynamoClient)
                .build();
        matches = localstackEnhancedClient.table("matches", TableSchema.fromBean(Match.class));
        matchesDao = new MatchesDao(localstackDynamoClient, localstackEnhancedClient);
    }

    @BeforeEach
    void purgeMatchTable() {
        environmentVariables.set("MATCHES_TABLE_NAME", "matches");
        try {
            log.info("Deleting table");
            log.info("Existing tables before delete: {}", localstackDynamoClient.listTables().tableNames());
            matches.deleteTable();
            waitForTableDelete();
        } catch (ResourceNotFoundException e) {
            log.info("Matches table does not exist");
        }
        log.info("Creating table");
        matches.createTable(builder -> builder
                .globalSecondaryIndices(
                        gsi -> gsi.indexName("getBySecondaryId")
                                .provisionedThroughput(throughput -> throughput.readCapacityUnits(1L).writeCapacityUnits(1L))
                                .projection(projection -> projection.projectionType(ProjectionType.ALL)),
                        gsi -> gsi.indexName("getByDate")
                                .provisionedThroughput(throughput -> throughput.writeCapacityUnits(1L).readCapacityUnits(1L))
                                .projection(projection -> projection.projectionType(ProjectionType.INCLUDE).nonKeyAttributes("home_team", "away_team")))
                .build());
        waitForTableCreated();
    }

    static void waitForTableCreated() {
        try(DynamoDbWaiter waiter = DynamoDbWaiter.builder().client(localstackDynamoClient).build()) {
            var response = waiter
                    .waitUntilTableExists(builder -> builder.tableName("matches").build())
                    .matched();
            DescribeTableResponse matchesCreated = response.response()
                    .orElseThrow(() -> new NoSuchElementException("Table matches was not created"));
            log.info("Matches table was created. Table name: {}", matchesCreated.table().tableName());
            log.info("Matches table: {}", matchesCreated.table());
        }
    }

    static void waitForTableDelete(){
        if(!localstackDynamoClient.listTables().tableNames().isEmpty()){
            try(DynamoDbWaiter waiter = DynamoDbWaiter.builder().client(localstackDynamoClient).build()) {
                var response = waiter
                        .waitUntilTableNotExists(builder -> builder.tableName("matches").build())
                        .matched();
                DescribeTableResponse matchesCreated = response.response()
                        .orElseThrow(() -> new NoSuchElementException("Table was not deleted. " + response.exception().get().getMessage()) );
            }
        }
        log.info("Matches table was deleted");
    }

    @Test
    void shouldReturnOneMatch_WhenOneDateIsMatching() {
        //given
        Match match = prepareMatchWithDate(LocalDateTime.of(2023, OCTOBER, 29, 11, 11));
        matches.putItem(match);
        Match matchFromDifferentDate = prepareMatchWithDate(LocalDateTime.of(2023, OCTOBER, 30, 11, 11));
        matches.putItem(matchFromDifferentDate);
        LocalDate matchDate = LocalDate.of(2023, OCTOBER, 29);
        //when
        List<Match> matchesFromDatabase = matchesDao.getByDate(matchDate);
        log.info("Matches fetched from database: {}", matchesFromDatabase.size());
        //then
        assertEquals(1, matchesFromDatabase.size());
    }

    @Test
    void shouldReturnTwoMatches_WhenTwoMatchTheDate() {
        //given
        Match match = prepareMatchWithDate(LocalDateTime.of(2023, OCTOBER, 29, 11, 11));
        matches.putItem(match);
        Match matchWithSameDate = prepareMatchWithDate(LocalDateTime.of(2023, OCTOBER, 29, 12, 12));
        matches.putItem(matchWithSameDate);
        Match matchFromDifferentDate = prepareMatchWithDate(LocalDateTime.of(2023, OCTOBER, 30, 11, 11));
        matches.putItem(matchFromDifferentDate);
        LocalDate matchDate = LocalDate.of(2023, OCTOBER, 29);
        //when
        List<Match> matchesFromDatabase = matchesDao.getByDate(matchDate);
        log.info("Matches fetched from database: {}", matchesFromDatabase.size());
        //then
        assertEquals(2, matchesFromDatabase.size());
    }

    @Test
    void shouldReturnOneMatch_WhenOneMatchesPrimaryId() {
        //given
        Match match = prepareMatchWithId("match-123");
        matches.putItem(match);
        //when
        Match matchFromDb = matchesDao.getById("match-123");
        //then
        assertEquals(match.getPrimaryId(), matchFromDb.getPrimaryId());
    }

    @Test
    void shouldSaveTwoTypes_WhenTwoDifferentMatchesPassed() throws Exception{
        //given
        Match match = prepareMatchWithId("match-123");
        Match match2 = prepareMatchWithId("match-124");
        //when
        BatchWriteResult writeResult = matchesDao.save(List.of(match, match2));
        //then
        assertEquals(2, matches.scan().items().stream().count());
    }

    @Test
    void shouldThrowException_WhenTwoSameMatchesPassed() {
        //given
        Match match = prepareMatchWithId("match-123");
        Match match2 = prepareMatchWithId("match-123");
        List<Match> filteredEntities = List.of(match, match2);
        //when, then
        assertThrows(DynamoDbException.class, () -> matchesDao.save(filteredEntities));
    }

    private Match prepareMatchWithId(String matchId) {
        Match match = prepareMatchWithDate(LocalDateTime.now());
        match.setPrimaryId(matchId);
        match.setSecondaryId(matchId);
        return match;
    }

    private Match prepareMatchWithDate(LocalDateTime date) {
        Random random = new Random();
        Match match = new Match();
        String id = "match-" + random.nextLong(100);
        match.setAwayScore(1 % 4);
        match.setHomeScore(1 % 2);
        match.setPrimaryId(id);
        match.setSecondaryId(id);
        match.setStartTime(date.toLocalTime());
        match.setDate(date.toLocalDate());
        match.setAwayTeam("team" + (1 + 1));
        match.setHomeTeam("team" + (1 + 2));
        return match;
    }
}