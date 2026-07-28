package com.anki.vocab_server.model;


import com.anki.vocab_server.enums.WordStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.util.UUID;


@Table("pending_queue_entry")
public class PendingQueueEntry {
    @Id
    private UUID id;
    private WordStatus status;
    private String word;
    private String lemma;
    private String source;
    private String ipa;
    private String pos;
    private String definition;
    private String example;
    @Column("audio_url")
    private String audioUrl;
    private String deck;
    private String topic;
    @Column("created_at")
    private LocalDate createdAt;
    @Column("approved_at")
    private LocalDate approvedAt;
}
