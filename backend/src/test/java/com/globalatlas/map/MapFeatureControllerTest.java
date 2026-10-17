package com.globalatlas.map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MapFeatureController.class)
class MapFeatureControllerTest {
 @Autowired MockMvc mvc;
 @MockBean JdbcTemplate jdbcTemplate;

 @Test void rejectsInvalidCountryIsoBeforeQuery() throws Exception {
  mvc.perform(get("/api/v1/map/countries/123/regions")).andExpect(status().isBadRequest());
  verifyNoInteractions(jdbcTemplate);
 }
 @Test void rejectsNegativeSimplificationTolerance() throws Exception {
  mvc.perform(get("/api/v1/map/countries").param("tolerance","-0.01")).andExpect(status().isBadRequest());
  verifyNoInteractions(jdbcTemplate);
 }
 @Test void rejectsExcessiveSimplificationTolerance() throws Exception {
  mvc.perform(get("/api/v1/map/countries").param("tolerance","0.6")).andExpect(status().isBadRequest());
  verifyNoInteractions(jdbcTemplate);
 }
 @Test void rejectsUnboundedRegionLimit() throws Exception {
  mvc.perform(get("/api/v1/map/regions").param("limit","5001")).andExpect(status().isBadRequest());
  verifyNoInteractions(jdbcTemplate);
 }
}
