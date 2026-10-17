package com.globalatlas.region;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RegionSearchController.class)
class RegionSearchControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RegionRepository repository;

    @Test
    void returnsSearchResultsForValidQuery() throws Exception {
        when(repository.search("teh", 20)).thenReturn(List.of(
                new RegionSearchResult(7L, "IR", "Iran", "ایران", "IR-07", "Tehran", "تهران", 1)
        ));

        mockMvc.perform(get("/api/v1/regions/search").param("q", "teh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].countryIso2").value("IR"))
                .andExpect(jsonPath("$[0].nameEn").value("Tehran"))
                .andExpect(jsonPath("$[0].nameFa").value("تهران"));

        verify(repository).search("teh", 20);
    }

    @Test
    void rejectsQueriesShorterThanTwoCharacters() throws Exception {
        mockMvc.perform(get("/api/v1/regions/search").param("q", "a"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsOutOfRangeLimit() throws Exception {
        mockMvc.perform(get("/api/v1/regions/search").param("q", "te").param("limit", "101"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(repository);
    }

    @Test
    void trimsQueryBeforeSearching() throws Exception {
        when(repository.search("te", 5)).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/regions/search").param("q", "  te  ").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        verify(repository).search("te", 5);
    }
}
