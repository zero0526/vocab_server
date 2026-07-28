package com.anki.vocab_server.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.Map;

@Table("providers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Provider {
    private Integer id;
    private String name;
    @Column("base_url")
    private String baseUrl;
    private boolean enable;
    @Column("created_at")
    private LocalDateTime createAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
    private Map<String, String> endpoint;
}
