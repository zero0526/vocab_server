package com.anki.vocab_server.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.util.UUID;


@Table("reference_words")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceWord {
    @Id
    private UUID id;
    private String word;
    private String lemma;
    private String source;
    @Column("cefr_level")
    private String cefrLevel;
    private String pos;
    @Column("imported_at")
    private LocalDate importedAt;
}
