package com.aparaitech.cet.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "results")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Result {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "attempt_id", unique = true)
    private ExamAttempt attempt;

    private Integer totalQuestions;
    private Integer attempted;
    private Integer correct;
    private Integer wrong;
    private Integer unanswered;

    private Double rawScore;      // correct*marks - wrong*negativeMarks
    private Double maxScore;
    private Double percentage;

    @CreationTimestamp
    private LocalDateTime calculatedAt;
}
