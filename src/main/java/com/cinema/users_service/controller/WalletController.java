package com.cinema.users_service.controller;

import com.cinema.users_service.constants.UsersConstants;
import com.cinema.users_service.dto.wallet.LegacyWalletDeductRequest;
import com.cinema.users_service.dto.wallet.WalletDeductRequest;
import com.cinema.users_service.dto.wallet.WalletRechargeRequest;
import com.cinema.users_service.dto.wallet.WalletResponse;
import com.cinema.users_service.service.HeaderAccessService;
import com.cinema.users_service.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class WalletController {

    private final WalletService walletService;
    private final HeaderAccessService headerAccessService;

    public WalletController(WalletService walletService, HeaderAccessService headerAccessService) {
        this.walletService = walletService;
        this.headerAccessService = headerAccessService;
    }

    @GetMapping("/users/wallet")
    public ResponseEntity<WalletResponse> getWallet(
            @RequestHeader(UsersConstants.HEADER_USER_ID) String userIdHeader
    ) {
        UUID userId = headerAccessService.parseUserId(userIdHeader);
        return ResponseEntity.ok(walletService.getWallet(userId));
    }

    @PostMapping("/users/wallet/recharge")
    public ResponseEntity<WalletResponse> recharge(
            @RequestHeader(UsersConstants.HEADER_USER_ID) String userIdHeader,
            @Valid @RequestBody WalletRechargeRequest request
    ) {
        UUID userId = headerAccessService.parseUserId(userIdHeader);
        return ResponseEntity.ok(walletService.recharge(userId, request.amount()));
    }

    @PostMapping("/users/wallet/deduct")
    public ResponseEntity<Void> deduct(
            @RequestHeader(UsersConstants.HEADER_INTERNAL_SERVICE) String internalHeader,
            @Valid @RequestBody WalletDeductRequest request
    ) {
        headerAccessService.requireInternalService(internalHeader);
        walletService.deduct(request.userId(), request.amount(), request.description());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/internal/wallets/{userId}/deduct")
    public ResponseEntity<Void> deductLegacy(
            @PathVariable UUID userId,
            @Valid @RequestBody LegacyWalletDeductRequest request
    ) {
        walletService.deduct(userId, request.amount(), "Legacy internal deduction");
        return ResponseEntity.ok().build();
    }
}