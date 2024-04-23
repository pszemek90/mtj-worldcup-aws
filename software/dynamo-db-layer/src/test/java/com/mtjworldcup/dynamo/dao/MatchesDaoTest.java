package com.mtjworldcup.dynamo.dao;

import com.mtjworldcup.dynamo.model.Match;
import com.mtjworldcup.dynamo.model.MatchStatus;
import com.mtjworldcup.dynamo.model.RecordType;
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
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import static java.time.Month.OCTOBER;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SystemStubsExtension.class)
@Testcontainers
class MatchesDaoTest {

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
            matches.deleteTable();
            waitForTableDelete();
        } catch (ResourceNotFoundException e) {
            log.info("Matches table does not exist");
        }
        matches.createTable(builder -> builder
                .globalSecondaryIndices(
                        gsi -> gsi.indexName("getBySecondaryId")
                                .provisionedThroughput(throughput -> throughput.readCapacityUnits(1L).writeCapacityUnits(1L))
                                .projection(projection -> projection.projectionType(ProjectionType.ALL)),
                        gsi -> gsi.indexName("getByDate")
                                .provisionedThroughput(throughput -> throughput.writeCapacityUnits(1L).readCapacityUnits(1L))
                                .projection(projection -> projection
                                        .projectionType(ProjectionType.ALL)),
                        gsi -> gsi.indexName("getByRecordType")
                                .provisionedThroughput(throughput -> throughput.writeCapacityUnits(1L).readCapacityUnits(1L))
                                .projection(projection -> projection
                                        .projectionType(ProjectionType.ALL))));
        waitForTableCreated();
    }

    static void waitForTableCreated() {
        try (DynamoDbWaiter waiter = DynamoDbWaiter.builder().client(localstackDynamoClient).build()) {
            var response = waiter
                    .waitUntilTableExists(builder -> builder.tableName("matches").build())
                    .matched();
            DescribeTableResponse matchesCreated = response.response()
                    .orElseThrow(() -> new NoSuchElementException("Table matches was not created"));
        }
    }

    static void waitForTableDelete() {
        if (!localstackDynamoClient.listTables().tableNames().isEmpty()) {
            try (DynamoDbWaiter waiter = DynamoDbWaiter.builder().client(localstackDynamoClient).build()) {
                var response = waiter
                        .waitUntilTableNotExists(builder -> builder.tableName("matches").build())
                        .matched();
                DescribeTableResponse matchesCreated = response.response()
                        .orElseThrow(() -> new NoSuchElementException("Table was not deleted. " + response.exception().get().getMessage()));
            }
        }
    }

    @Test
    void shouldReturnOneMatch_WhenOneDateIsMatching() {
        //given
        Match match = prepareEntity();
        match.setDate(LocalDate.of(2023, OCTOBER, 29));
        match.setStartTime(LocalTime.of(11, 11));
        matches.putItem(match);
        Match matchFromDifferentDate = prepareEntity();
        matchFromDifferentDate.setDate(LocalDate.of(2023, OCTOBER, 30));
        matchFromDifferentDate.setStartTime(LocalTime.of(11, 11));
        matches.putItem(matchFromDifferentDate);
        LocalDate matchDate = LocalDate.of(2023, OCTOBER, 29);
        //when
        List<Match> matchesFromDatabase = matchesDao.getByDate(matchDate);
        //then
        assertEquals(1, matchesFromDatabase.size());
        assertEquals(LocalTime.of(11, 11), matchesFromDatabase.get(0).getStartTime());
    }

    @Test
    void shouldReturnTwoMatches_WhenTwoMatchTheDate() {
        //given
        Match match = prepareEntity();
        match.setDate(LocalDate.of(2023, OCTOBER, 29));
        match.setStartTime(LocalTime.of(11, 11));
        matches.putItem(match);
        Match matchWithSameDate = prepareEntity();
        matchWithSameDate.setDate(LocalDate.of(2023, OCTOBER, 29));
        matchWithSameDate.setStartTime(LocalTime.of(12, 12));
        matches.putItem(matchWithSameDate);
        Match matchFromDifferentDate = prepareEntity();
        matchFromDifferentDate.setDate(LocalDate.of(2023, OCTOBER, 30));
        matchFromDifferentDate.setStartTime(LocalTime.of(11, 11));
        matches.putItem(matchFromDifferentDate);
        LocalDate matchDate = LocalDate.of(2023, OCTOBER, 29);
        //when
        List<Match> matchesFromDatabase = matchesDao.getByDate(matchDate);
        //then
        assertEquals(2, matchesFromDatabase.size());
    }

    @Test
    void shouldReturnOneMatch_WhenOneMatchesPrimaryId() {
        //given
        Match match = prepareEntity();
        match.setPrimaryId("match-123");
        match.setSecondaryId("match-123");
        matches.putItem(match);
        //when
        Match matchFromDb = matchesDao.getById("match-123");
        //then
        assertEquals(match.getPrimaryId(), matchFromDb.getPrimaryId());
    }

    @Test
    void shouldSaveTwoTypes_WhenTwoDifferentMatchesPassed() throws Exception {
        //given
        String match123Id = "match-123";
        Match match = prepareEntity();
        match.setPrimaryId(match123Id);
        match.setSecondaryId(match123Id);
        String match124Id = "match-124";
        Match match2 = prepareEntity();
        match2.setPrimaryId(match124Id);
        match2.setSecondaryId(match124Id);
        String user123Id = "user-123";
        Match user123 = prepareEntity();
        user123.setPrimaryId(user123Id);
        user123.setSecondaryId(user123Id);
        user123.setRecordType(RecordType.USER);
        user123.setPool(new BigDecimal(50, new MathContext(2)));
        matches.putItem(user123);
        matches.putItem(match);
        matches.putItem(match2);
        Match typing1 = prepareEntity();
        typing1.setPrimaryId(match123Id);
        typing1.setSecondaryId(user123Id);
        typing1.setRecordType(RecordType.TYPING);
        Match typing2 = prepareEntity();
        typing2.setPrimaryId(match124Id);
        typing2.setSecondaryId(user123Id);
        typing2.setRecordType(RecordType.TYPING);
        //when
        matchesDao.saveTypings(List.of(typing1, typing2));
        //then
        List<Match> entities = matches.scan().items().stream().toList();
        List<Match> typings = entities.stream().filter(entity -> entity.getRecordType().equals(RecordType.TYPING)).toList();
        List<Match> matches = entities.stream().filter(entity -> entity.getRecordType().equals(RecordType.MATCH)).toList();
        List<Match> users = entities.stream().filter(entity -> entity.getRecordType().equals(RecordType.USER)).toList();
        assertEquals(2, typings.size());
        assertEquals(2, matches.size());
        assertEquals(1, users.size());
        assertEquals(48, users.get(0).getPool().intValue());
        assertEquals(1, matches.get(0).getPool().intValue());
        assertEquals(1, matches.get(1).getPool().intValue());
    }

    @Test
    void shouldChangeBalanceOnceAndScoresTwice_WhenTheSameTypePassedTwice() throws Exception {
        //given
        String match123Id = "match-123";
        Match match = prepareEntity();
        match.setPrimaryId(match123Id);
        match.setSecondaryId(match123Id);
        String user123Id = "user-123";
        Match user123 = prepareEntity();
        user123.setPrimaryId(user123Id);
        user123.setSecondaryId(user123Id);
        user123.setRecordType(RecordType.USER);
        user123.setPool(new BigDecimal(50));
        matches.putItem(user123);
        matches.putItem(match);
        Match typing1 = prepareEntity();
        typing1.setPrimaryId(match123Id);
        typing1.setSecondaryId(user123Id);
        typing1.setHomeScore(1);
        typing1.setAwayScore(1);
        typing1.setRecordType(RecordType.TYPING);
        Match typing2 = prepareEntity();
        typing2.setPrimaryId(match123Id);
        typing2.setSecondaryId(user123Id);
        typing2.setHomeScore(2);
        typing2.setAwayScore(2);
        typing2.setRecordType(RecordType.TYPING);
        //when
        matchesDao.saveTypings(List.of(typing1, typing2));
        //then
        List<Match> entities = matches.scan().items().stream().toList();
        List<Match> typings = entities.stream().filter(entity -> entity.getRecordType().equals(RecordType.TYPING)).toList();
        List<Match> matches = entities.stream().filter(entity -> entity.getRecordType().equals(RecordType.MATCH)).toList();
        List<Match> users = entities.stream().filter(entity -> entity.getRecordType().equals(RecordType.USER)).toList();
        assertEquals(1, typings.size());
        assertEquals(2, typings.get(0).getHomeScore());
        assertEquals(2, typings.get(0).getAwayScore());
        assertEquals(49, users.get(0).getPool().intValue());
        assertEquals(1, matches.get(0).getPool().intValue());
    }

    @Test
    void shouldReturnOneMatch_WhenOnlyOneMatchFinished() {
        Match match = prepareEntity();
        match.setMatchStatus(MatchStatus.FINISHED);
        match.setPrimaryId("match-123");
        matches.putItem(match);
        Match match1 = prepareEntity();
        match1.setPrimaryId("match-124");
        match1.setAwayScore(null);
        match1.setHomeScore(null);
        matches.putItem(match1);
        //when
        List<Match> matchesFromDatabase = matchesDao.getFinishedMatches();
        //then
        assertEquals(1, matchesFromDatabase.size());
    }

    @Test
    void shouldReturnEmptyList_WhenNoMatchesFinished() {
        Match match = prepareEntity();
        match.setPrimaryId("match-123");
        matches.putItem(match);
        Match match1 = prepareEntity();
        match1.setPrimaryId("match-124");
        matches.putItem(match1);
        //when
        List<Match> finishedMatches = matchesDao.getFinishedMatches();
        //then
        assertEquals(0, finishedMatches.size());
    }

    @Test
    void shouldReturnNoTypingsForUser_WhenNoTypingsInDb() {
        //given
        String userId = "user-123";
        //when
        List<Match> typings = matchesDao.getTypings(userId);
        //then
        assertEquals(0, typings.size());
    }

    @Test
    void shouldReturnOneTyping_WhenOneTypingInDb() {
        //given
        String userId = "user-123";
        Match typing = prepareEntity();
        typing.setSecondaryId(userId);
        typing.setRecordType(RecordType.TYPING);
        matches.putItem(typing);
        //when
        List<Match> typings = matchesDao.getTypings(userId);
        //then
        assertEquals(1, typings.size());
    }

    @Test
    void shouldReturnOneTyping_WhenOneTypingForUserInDb() {
        //given
        String userId = "user-123";
        Match typing = prepareEntity();
        typing.setSecondaryId(userId);
        typing.setRecordType(RecordType.TYPING);
        matches.putItem(typing);
        String differentUser = "user-124";
        Match differentTyping = prepareEntity();
        differentTyping.setSecondaryId(differentUser);
        differentTyping.setRecordType(RecordType.TYPING);
        matches.putItem(differentTyping);
        //when
        List<Match> typings = matchesDao.getTypings(userId);
        //then
        assertEquals(1, typings.size());
    }

    @Test
    void shouldReturnNoTypings_WhenNoTypingsInDb() {
        //when
        List<Match> typings = matchesDao.getAllTypings();
        //then
        assertEquals(0, typings.size());
    }

    @Test
    void shouldReturnTwoTypings_WhenTwoTypingsInDb() {
        //given
        Match typing = prepareEntity();
        typing.setRecordType(RecordType.TYPING);
        typing.setSecondaryId("user-123");
        matches.putItem(typing);
        Match typing2 = prepareEntity();
        typing2.setRecordType(RecordType.TYPING);
        typing2.setSecondaryId("user-124");
        matches.putItem(typing2);
        Match match = prepareEntity();
        matches.putItem(match);
        //when
        List<Match> typings = matchesDao.getAllTypings();
        //then
        assertEquals(2, typings.size());
    }

    @Test
    void shouldReturn100OverallPool_When100InDb() {
        //given
        Match overallPool = prepareEntity();
        overallPool.setPrimaryId("overall_pool");
        overallPool.setSecondaryId("overall_pool");
        overallPool.setPool(new BigDecimal(100));
        overallPool.setRecordType(RecordType.POOL);
        matches.putItem(overallPool);
        //when
        Match overallPoolFromDb = matchesDao.getOverallPool();
        //then
        assertEquals(100, overallPoolFromDb.getPool().intValue());
    }

    @Test
    void shouldReturn200OverallPool_When200InDb() {
        //given
        Match overallPool = prepareEntity();
        overallPool.setPrimaryId("overall_pool");
        overallPool.setSecondaryId("overall_pool");
        overallPool.setPool(new BigDecimal(200));
        overallPool.setRecordType(RecordType.POOL);
        matches.putItem(overallPool);
        //when
        Match overallPoolFromDb = matchesDao.getOverallPool();
        //then
        assertEquals(200, overallPoolFromDb.getPool().intValue());
    }

    @Test
    void shouldUpdateMatchStatus_WhenMatchStatusHasChanged() {
        // given
        Match match = prepareEntity();
        match.setMatchStatus(MatchStatus.SCHEDULED);
        matches.putItem(match);
        match.setMatchStatus(MatchStatus.FINISHED);
        // when
        matchesDao.update(match);
        // then
        Match matchFromDb = matchesDao.getById(match.getPrimaryId());
        assertEquals(MatchStatus.FINISHED, matchFromDb.getMatchStatus());
    }

    private Match prepareEntity() {
        Random random = new Random();
        Match match = new Match();
        LocalDateTime date = LocalDateTime.now();
        String id = "match-" + random.nextLong(100);
        match.setAwayScore(1 % 4);
        match.setHomeScore(1 % 2);
        match.setPrimaryId(id);
        match.setSecondaryId(id);
        match.setStartTime(date.toLocalTime());
        match.setDate(date.toLocalDate());
        match.setAwayTeam("team" + (1 + 1));
        match.setHomeTeam("team" + (1 + 2));
        match.setRecordType(RecordType.MATCH);
        match.setMatchStatus(MatchStatus.SCHEDULED);
        match.setPool(new BigDecimal(0));
        match.setCorrectTypings(0);
        return match;
    }
}