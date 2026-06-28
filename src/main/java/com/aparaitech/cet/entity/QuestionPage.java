package com.aparaitech.cet.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "question_pages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "question_paper_id")
    private QuestionPaper questionPaper;

    private Integer pageNumber;       // 1-based page in PDF
    private Integer questionNumber;   // which question this page represents
    private String imageFileName;     // e.g. exam1_page1.png
    private String imagePath;         // relative path for serving
}
