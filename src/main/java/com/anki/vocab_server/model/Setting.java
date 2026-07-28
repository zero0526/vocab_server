package com.anki.vocab_server.model;

import lombok.Data;
import org.springframework.data.relational.core.mapping.Table;

@Table("settings")
@Data
public class Setting {
    private String key;
    private String value;
}
