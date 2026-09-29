package com.honeygroup.honeylms.course;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentFileId implements Serializable {

    private Long assignmentId;
    private Long storedFileId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AssignmentFileId that)) return false;
        return Objects.equals(assignmentId, that.assignmentId)
                && Objects.equals(storedFileId, that.storedFileId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assignmentId, storedFileId);
    }
}
