package com.cinema.users_service.controller;

import com.cinema.users_service.constants.UsersConstants;
import com.cinema.users_service.dto.wallet.WalletResponse;
import com.cinema.users_service.exception.GlobalExceptionHandler;
import com.cinema.users_service.exception.InsufficientBalanceException;
import com.cinema.users_service.service.HeaderAccessService;
import com.cinema.users_service.service.WalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalletController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WalletService walletService;

    @MockBean
    private HeaderAccessService headerAccessService;

    @Test
    void getWalletShouldReturn200() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(headerAccessService.parseUserId("header-user")).thenReturn(userId);
        when(walletService.getWallet(userId)).thenReturn(new WalletResponse(new BigDecimal("10.00"), List.of()));

        // Act
        // Assert
        mockMvc.perform(get("/users/wallet")
                        .header(UsersConstants.HEADER_USER_ID, "header-user"))
                .andExpect(status().isOk());
    }

    @Test
    void rechargeValidShouldReturn200() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(headerAccessService.parseUserId("header-user")).thenReturn(userId);
        when(walletService.recharge(any(UUID.class), any(BigDecimal.class)))
                .thenReturn(new WalletResponse(new BigDecimal("20.00"), List.of()));

        // Act
        // Assert
        mockMvc.perform(post("/users/wallet/recharge")
                        .header(UsersConstants.HEADER_USER_ID, "header-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":5.00}"))
                .andExpect(status().isOk());
    }

    @Test
    void deductWithSufficientBalanceShouldReturn200() throws Exception {
        // Arrange
        doNothing().when(headerAccessService).requireInternalService(UsersConstants.HEADER_INTERNAL_SERVICE_VALUE);
        doNothing().when(walletService).deduct(any(UUID.class), any(BigDecimal.class), anyString());

        // Act
        // Assert
        mockMvc.perform(post("/users/wallet/deduct")
                        .header(UsersConstants.HEADER_INTERNAL_SERVICE, UsersConstants.HEADER_INTERNAL_SERVICE_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"550e8400-e29b-41d4-a716-446655440000\",\"amount\":5.00,\"description\":\"Pago\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void deductWithoutBalanceShouldReturn400() throws Exception {
        // Arrange
        doNothing().when(headerAccessService).requireInternalService(UsersConstants.HEADER_INTERNAL_SERVICE_VALUE);
        doThrow(new InsufficientBalanceException("Saldo insuficiente"))
                .when(walletService).deduct(any(UUID.class), any(BigDecimal.class), anyString());

        // Act
        // Assert
        mockMvc.perform(post("/users/wallet/deduct")
                        .header(UsersConstants.HEADER_INTERNAL_SERVICE, UsersConstants.HEADER_INTERNAL_SERVICE_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"550e8400-e29b-41d4-a716-446655440000\",\"amount\":5.00,\"description\":\"Pago\"}"))
                .andExpect(status().isBadRequest());
    }
}