package com.mtjworldcup.getalltypings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mtjworldcup.getalltypings.model.MatchTyping;
import com.mtjworldcup.getalltypings.model.TypingDto;
import com.mtjworldcup.getalltypings.service.TypingsService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class HandlerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ObjectMapper SPY_OBJECT_MAPPER = spy(ObjectMapper.class).registerModule(new JavaTimeModule());
    private final TypingsService MOCK_TYPINGS_SERVICE = mock(TypingsService.class);

    @BeforeAll
    static void setup() {
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
    }

    @Test
    void shouldReturnNoTypings_WhenNoTypingsAvailable() throws Exception {
        //given
        Map<LocalDate, Map<String, Set<TypingDto>>> allTypings = Map.of();
        when(MOCK_TYPINGS_SERVICE.getAllTypings()).thenReturn(allTypings);
        var handler = new Handler(SPY_OBJECT_MAPPER, MOCK_TYPINGS_SERVICE);
        //when
        var response = handler.handleRequest(null, null);
        //then
        String rawBody = response.getBody();
        var responseTypings = OBJECT_MAPPER.readValue(rawBody, new TypeReference<Map<LocalDate, List<MatchTyping>>>() {});
        assertEquals(0, responseTypings.size());
        assertEquals(200, response.getStatusCode());
    }

    @Test
    void shouldReturnOneTyping_WhenOneTypingAvailable() throws Exception {
        //given
        var localDate = LocalDate.of(2024, 4, 6);
        TypingDto typingDto = new TypingDto(localDate, "Poland - Brazil", "user-123", "2-1", true);
        var dateTypings = Map.of("Poland - Brazil", Set.of(typingDto));
        when(MOCK_TYPINGS_SERVICE.getAllTypings()).thenReturn(Map.of(localDate, dateTypings));
        var handler = new Handler(SPY_OBJECT_MAPPER, MOCK_TYPINGS_SERVICE);
        //when
        var response = handler.handleRequest(null, null);
        //then
        String rawBody = response.getBody();
        var allTypings = OBJECT_MAPPER.readValue(rawBody, new TypeReference<Map<LocalDate, Map<String, Set<TypingDto>>>>() {});
        assertEquals(1, allTypings.size());
        assertEquals(dateTypings, allTypings.get(localDate));
        assertEquals(200, response.getStatusCode());
    }

    @Test
    void shouldReturnServerError_WhenJsonProcessingException() throws Exception {
        //given
        Map<LocalDate, Map<String, Set<TypingDto>>> allTypings = Map.of();
        when(MOCK_TYPINGS_SERVICE.getAllTypings()).thenReturn(allTypings);
        when(SPY_OBJECT_MAPPER.writeValueAsString(allTypings)).thenThrow(
                new JsonProcessingException("JsonProcessingException") {});
        var handler = new Handler(SPY_OBJECT_MAPPER, MOCK_TYPINGS_SERVICE);
        //when
        var response = handler.handleRequest(null, null);
        //then
        assertEquals(500, response.getStatusCode());
        assertEquals("Server error occurred. Please contact support.", response.getBody());
    }

    @Test
    void shouldReturnServerError_WhenUnexpectedException() throws Exception {
        //given
        when(MOCK_TYPINGS_SERVICE.getAllTypings()).thenThrow(new RuntimeException("RuntimeException"));
        var handler = new Handler(SPY_OBJECT_MAPPER, MOCK_TYPINGS_SERVICE);
        //when
        var response = handler.handleRequest(null, null);
        //then
        assertEquals(500, response.getStatusCode());
        assertEquals("Server error occurred. Please contact support.", response.getBody());
    }

}