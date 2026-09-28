package com.honeygroup.honeylms.enrollment;

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
public class EnrollmentId implements Serializable {

    private Long studentUserAccountId;
    private Long courseId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EnrollmentId that)) return false;
        return Objects.equals(studentUserAccountId, that.studentUserAccountId)
                && Objects.equals(courseId, that.courseId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentUserAccountId, courseId);
    }
}
