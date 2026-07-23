package com.bankplatform.account.adapter.in.web.dto;

import com.bankplatform.account.domain.Account;
import java.util.List;
import org.springframework.data.domain.Page;

public record AccountListResponse(
        List<AccountResponse> items, int page, int size, long totalElements, int totalPages) {

    public static AccountListResponse from(Page<Account> page) {
        return new AccountListResponse(
                page.getContent().stream().map(AccountResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
