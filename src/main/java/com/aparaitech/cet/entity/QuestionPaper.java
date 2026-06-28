package com.aparaitech.cet.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "question_papers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionPaper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "exam_id", unique = true)
    private Exam exam;

    private String originalFileName;
    private String storedFileName;
    private String filePath;
    private Integer totalPages;

    @OneToMany(mappedBy = "questionPaper", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("pageNumber ASC")
    @Builder.Default
    private List<QuestionPage> pages = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime uploadedAt;
}
