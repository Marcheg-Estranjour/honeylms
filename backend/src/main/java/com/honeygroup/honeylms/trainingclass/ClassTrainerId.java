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
public class ClassTrainerId implements Serializable {

    private Long trainerUserAccountId;
    private Long classId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClassTrainerId that)) return false;
        return Objects.equals(trainerUserAccountId, that.trainerUserAccountId)
                && Objects.equals(classId, that.classId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trainerUserAccountId, classId);
    }
}
