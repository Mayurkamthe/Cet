package com.aparaitech.cet.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "student_answers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "attempt_id", nullable = false)
    private ExamAttempt attempt;

    private Integer questionNumber;  // 1-based
    private String selectedOption;   // A, B, C, D or null

    @Builder.Default
    private boolean markedForReview = false;
}
