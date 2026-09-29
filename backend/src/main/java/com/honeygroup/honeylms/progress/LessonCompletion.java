package com.honeygroup.honeylms.progress;

import com.honeygroup.honeylms.course.Lesson;
import com.honeygroup.honeylms.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tracks a Student's consultation and completion of a Lesson. A row can exist
 * with only lastViewedAt set (completedAt null) - that's how "resume where I
 * left off" (US-PROGRESS-04) works without a dedicated column elsewhere.
 * No created_at/updated_at: these two timestamps already represent everything
 * useful about this row's state (Dossier de Conception §20).
 */
@Entity
@Table(name = "lesson_completion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonCompletion {

    @EmbeddedId
    private LessonCompletionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("studentUserAccountId")
    @JoinColumn(name = "student_user_account_id")
    private UserAccount student;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("lessonId")
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "last_viewed_at")
    private LocalDateTime lastViewedAt;

    public static LessonCompletion newFor(UserAccount student, Lesson lesson) {
        LessonCompletion completion = new LessonCompletion();
        completion.setId(new LessonCompletionId(student.getId(), lesson.getId()));
        completion.setStudent(student);
        completion.setLesson(lesson);
        return completion;
    }
}
