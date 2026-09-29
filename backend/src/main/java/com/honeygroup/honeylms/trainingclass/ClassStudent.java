package com.honeygroup.honeylms.trainingclass;

import com.honeygroup.honeylms.user.UserAccount;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Never used as an access-control mechanism - see TrainingClass javadoc. */
@Entity
@Table(name = "class_student")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassStudent {

    @EmbeddedId
    private ClassStudentId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("studentUserAccountId")
    @JoinColumn(name = "student_user_account_id")
    private UserAccount student;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("classId")
    @JoinColumn(name = "class_id")
    private TrainingClass trainingClass;

    public static ClassStudent of(TrainingClass trainingClass, UserAccount student) {
        ClassStudent cs = new ClassStudent();
        cs.setId(new ClassStudentId(student.getId(), trainingClass.getId()));
        cs.setStudent(student);
        cs.setTrainingClass(trainingClass);
        return cs;
    }
}
