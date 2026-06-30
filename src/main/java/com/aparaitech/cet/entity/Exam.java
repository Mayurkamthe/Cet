package com.aparaitech.cet.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exams")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String title;

    private String description;

    @NotBlank
    private String examType; // CET, NEET, JEE

    @NotNull
    private Integer durationMinutes;

    @NotNull
    private Integer totalQuestions;

    private Integer marksPerCorrect;
    private Integer negativeMarks; // stored as positive, applied as negative

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ExamStatus status = ExamStatus.DRAFT;

    private LocalDateTime scheduledAt; // legacy, kept for backward compatibility

    private LocalDateTime startAt;
    private LocalDateTime endAt;

    @OneToOne(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    private QuestionPaper questionPaper;

    @OneToOne(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    private AnswerKey answerKey;

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExamAttempt> attempts = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum ExamStatus {
        DRAFT, PUBLISHED, CLOSED
    }
}
