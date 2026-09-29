package com.honeygroup.honeylms.trainingclass.dto;

import java.util.List;

public record ClassMembersResponse(
        List<ClassMemberSummary> students,
        List<ClassMemberSummary> trainers
) {
}
