package com.anki.vocab_server.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class WordSense {
    private UUID id;
    private String phrase;
    private String guideWord;
    private String cefr;
    private String definition;
    private List<String> examples;
    private List<String> grammarLabels;
    private List<Collocation> collocations;
}
