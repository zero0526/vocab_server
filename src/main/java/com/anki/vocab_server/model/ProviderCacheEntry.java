package com.anki.vocab_server.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Table("provider_cache_entry")
public class ProviderCacheEntry {
    @Id
    private UUID id;
    private String word;
    @Column("provider_name")
    private String providerName;
    @Column("response_json")
    private String responseJson;
    @Column("fetched_at")
    private LocalDateTime fetchedAt;
    @Column("expires_at")
    private LocalDateTime expiredAt;
}
