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
public class CourseTrainerId implements Serializable {

    private Long courseId;
    private Long trainerUserAccountId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CourseTrainerId that)) return false;
        return Objects.equals(courseId, that.courseId)
                && Objects.equals(trainerUserAccountId, that.trainerUserAccountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(courseId, trainerUserAccountId);
    }
}
