package com.honeygroup.honeylms.trainingclass.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateClassRequest(
        @NotBlank(message = "Name is required") String name,
        String description
) {
}
