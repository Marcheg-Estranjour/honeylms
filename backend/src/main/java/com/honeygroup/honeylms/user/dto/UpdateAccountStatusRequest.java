package com.honeygroup.honeylms.user.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateAccountStatusRequest(
        @NotNull(message = "active is required") Boolean active
) {
}
