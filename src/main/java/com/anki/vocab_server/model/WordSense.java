package com.anki.vocab_server.model;

import java.util.List;
import java.util.UUID;

public class WordSense {
    private UUID id;
    private String guideWord;
    private String cefr;
    private String definition;
    private List<String> examples;
    private List<String> grammarLabels;
    private List<Collocation> collocations;
}
