package com.honeygroup.honeylms.trainingclass;

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
public class ClassStudentId implements Serializable {

    private Long studentUserAccountId;
    private Long classId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClassStudentId that)) return false;
        return Objects.equals(studentUserAccountId, that.studentUserAccountId)
                && Objects.equals(classId, that.classId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentUserAccountId, classId);
    }
}
