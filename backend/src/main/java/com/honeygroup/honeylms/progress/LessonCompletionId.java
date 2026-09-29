package com.honeygroup.honeylms.progress;

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
public class LessonCompletionId implements Serializable {

    private Long studentUserAccountId;
    private Long lessonId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LessonCompletionId that)) return false;
        return Objects.equals(studentUserAccountId, that.studentUserAccountId)
                && Objects.equals(lessonId, that.lessonId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentUserAccountId, lessonId);
    }
}
