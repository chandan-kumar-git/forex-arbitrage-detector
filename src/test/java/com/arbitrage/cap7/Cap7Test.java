package com.arbitrage.cap7;

import com.arbitrage.cap7.dto.Dtos.CurrencyDto;
import com.arbitrage.cap7.dto.Dtos.CurrencyRequest;
import com.arbitrage.cap7.rest.ApiExceptionHandler;
import com.arbitrage.cap7.rest.CurrencyController;
import com.arbitrage.cap7.service.ConflictException;
import com.arbitrage.cap7.service.CurrencyCrudService;
import com.arbitrage.cap7.service.NotFoundException;
import com.arbitrage.cap7.service.RouteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CAP-7: the REST controller and its error handler with MockMvc (no server, no MySQL:
 * the service is a Mockito mock), plus the route planner's shortest path.
 */
class Cap7Test {

    private CurrencyCrudService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(CurrencyCrudService.class);
        mvc = MockMvcBuilders.standaloneSetup(new CurrencyController(service))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test
    void listReturnsJson() throws Exception {
        when(service.list()).thenReturn(List.of(new CurrencyDto("USD", "US Dollar", 1.0, true)));
        mvc.perform(get("/api/v1/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("USD"));
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        when(service.create(any(CurrencyRequest.class))).thenReturn(new CurrencyDto("AUD", "Australian Dollar", 0.66, true));
        mvc.perform(post("/api/v1/currencies").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"AUD\",\"name\":\"Australian Dollar\",\"usdPrice\":0.66}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/currencies/AUD"));
    }

    @Test
    void invalidBodyReturns400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/v1/currencies").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"aud\",\"name\":\"A\",\"usdPrice\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.code").exists())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.usdPrice").exists());
    }

    @Test
    void duplicateReturns409AndMissingReturns404() throws Exception {
        when(service.create(any(CurrencyRequest.class))).thenThrow(new ConflictException("Currency AUD already exists"));
        mvc.perform(post("/api/v1/currencies").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"AUD\",\"name\":\"Australian Dollar\",\"usdPrice\":0.66}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Currency AUD already exists"));

        when(service.get("XYZ")).thenThrow(new NotFoundException("Currency XYZ does not exist"));
        mvc.perform(get("/api/v1/currencies/XYZ")).andExpect(status().isNotFound());
    }

    @Test
    @SuppressWarnings("unchecked")
    void routePlannerGoesThroughUsdWhenTheDirectPairIsExpensive() throws Exception {
        // 0 = INR, 1 = USD, 2 = JPY; the direct INR -> JPY rate is worse than via USD
        double[][] rate = {
                {1, 0.0119, 1.70},
                {83.0, 1, 149.0},
                {0.0066, 0.0067, 1}};
        Method best = RouteService.class.getDeclaredMethod("bestPath", double[][].class, int.class, int.class);
        best.setAccessible(true);
        List<Integer> path = (List<Integer>) best.invoke(null, rate, 0, 2);
        assertEquals(List.of(0, 1, 2), path);     // 0.0119 * 149 = 1.773 beats 1.70
    }
}
