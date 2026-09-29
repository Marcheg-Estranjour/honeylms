package com.honeygroup.honeylms.trainingclass.dto;

public record ClassMemberSummary(
        Long userId,
        String firstName,
        String lastName,
        String email
) {
}
