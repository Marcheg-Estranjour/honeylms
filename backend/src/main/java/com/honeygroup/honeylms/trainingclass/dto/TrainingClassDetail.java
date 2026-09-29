package com.honeygroup.honeylms.trainingclass.dto;

public record TrainingClassDetail(
        Long id,
        Long courseId,
        String name,
        String description
) {
}
