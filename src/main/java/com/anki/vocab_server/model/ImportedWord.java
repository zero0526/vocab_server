package com.anki.vocab_server.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.util.UUID;

@Table("imported_word")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportedWord {
    @Id
    private UUID id;
    private String word;
    private String lemma;
    @Column("anki_note_id")
    private Integer ankiNoteId;
    private String deck;
    private String topic;
    @Column("imported_at")
    private LocalDate importedAt;
}
