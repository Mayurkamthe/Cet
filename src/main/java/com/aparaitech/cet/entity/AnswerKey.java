package com.aparaitech.cet.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "answer_keys")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "exam_id", unique = true)
    private Exam exam;

    // JSON-like comma-separated answers stored as string
    // e.g. "A,B,C,D,A,B,..." for questions 1,2,3...
    @Column(columnDefinition = "TEXT")
    private String answers;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Returns list of answers ["A","B","C",...] ordered by question number.
     */
    public List<String> getAnswerList() {
        List<String> list = new ArrayList<>();
        if (answers != null && !answers.isBlank()) {
            for (String a : answers.split(",")) {
                list.add(a.trim());
            }
        }
        return list;
    }

    public void setAnswerList(List<String> list) {
        this.answers = String.join(",", list);
    }
}
