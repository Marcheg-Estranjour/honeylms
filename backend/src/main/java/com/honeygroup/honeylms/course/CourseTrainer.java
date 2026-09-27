package com.honeygroup.honeylms.course;

import com.honeygroup.honeylms.user.UserAccount;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Defines a Trainer's authorization perimeter: "this Trainer may manage this Course".
 * Distinct from ClassTrainer (added later), which is about supervising a real group
 * of people, not about content-editing rights.
 */
@Entity
@Table(name = "course_trainer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseTrainer {

    @EmbeddedId
    private CourseTrainerId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("courseId")
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("trainerUserAccountId")
    @JoinColumn(name = "trainer_user_account_id")
    private UserAccount trainer;

    public static CourseTrainer of(Course course, UserAccount trainer) {
        CourseTrainer courseTrainer = new CourseTrainer();
        courseTrainer.setId(new CourseTrainerId(course.getId(), trainer.getId()));
        courseTrainer.setCourse(course);
        courseTrainer.setTrainer(trainer);
        return courseTrainer;
    }
}
