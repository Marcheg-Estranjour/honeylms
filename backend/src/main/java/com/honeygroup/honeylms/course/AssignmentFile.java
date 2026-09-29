package com.honeygroup.honeylms.course;

import com.honeygroup.honeylms.file.StoredFile;
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

/** Join table: an Assignment can have several attached StoredFile (US-ASSIGN-03). */
@Entity
@Table(name = "assignment_file")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentFile {

    @EmbeddedId
    private AssignmentFileId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("assignmentId")
    @JoinColumn(name = "assignment_id")
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("storedFileId")
    @JoinColumn(name = "stored_file_id")
    private StoredFile storedFile;

    public static AssignmentFile of(Assignment assignment, StoredFile storedFile) {
        AssignmentFile af = new AssignmentFile();
        af.setId(new AssignmentFileId(assignment.getId(), storedFile.getId()));
        af.setAssignment(assignment);
        af.setStoredFile(storedFile);
        return af;
    }
}
