package com.anki.vocab_server.model;


import com.anki.vocab_server.enums.WordStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


@Table("pending_queue_entry")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingQueueEntry {
    @Id
    private UUID id;
    private WordStatus status;
    private String word;
    private String lemma;
    private String dictionary;
    private String pos;
    private String ipaUs;
    private String ipaUk;
    @Column("audio_url_us")
    private String audioUrlUs;
    @Column("audio_url_uk")
    private String audioUrlUk;
    private List<String> decks;
    private List<String> topics;
    private List<WordSense> wordSenses;
    private String plural;
    private String past;
    private String pastParticiple;
    private String presentParticiple;
    private String superlative;
    private String comparative;
    @Column("created_at")
    private LocalDate createdAt;
    @Column("approved_at")
    private LocalDate approvedAt;
}
