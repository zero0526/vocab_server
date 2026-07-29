package com.anki.vocab_server.provider;

import com.anki.vocab_server.dtos.proxy.Proxy;
import com.anki.vocab_server.enums.WordStatus;
import com.anki.vocab_server.model.Collocation;
import com.anki.vocab_server.model.PendingQueueEntry;
import com.anki.vocab_server.model.WordSense;
import com.anki.vocab_server.service.proxy.ProxyManager;
import com.anki.vocab_server.utils.HttpProxyUtils;
import com.anki.vocab_server.utils.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class CambridgeProvider {
    private final ProxyManager proxyManager;

    @Value("${cambridge.base-url:https://dictionary.cambridge.org/dictionary/english/}")
    private String cambridgeBaseUrl;

    public List<PendingQueueEntry> crawlCambridge(String word) {
        if (word == null || word.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String targetUrl = cambridgeBaseUrl + word.trim().toLowerCase();
        Proxy proxy = (proxyManager != null) ? proxyManager.acquireRandom() : null;

        Request request = new Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept-Language", "en-US,en;q=0.9")
                .get()
                .build();

        try (Response response = HttpProxyUtils.executeRequest(request, proxy)) {
            if (!response.isSuccessful() || response.body() == null) {
                log.warn("Failed to fetch Cambridge page for word '{}', status code: {}", word, response.code());
                return Collections.emptyList();
            }

            String html = response.body().string();
            return parseCambridgePage(html, word);
        } catch (IOException e) {
            log.error("Error crawling Cambridge for word '{}': {}", word, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private List<PendingQueueEntry> parseCambridgePage(String html, String word) {
        List<PendingQueueEntry> results = new ArrayList<>();
        if (html == null || html.isEmpty()) {
            return results;
        }

        Document doc = Jsoup.parse(html);
        Element dictionary = doc.selectFirst("div.pr.dictionary");
        if (dictionary == null) {
            dictionary = doc;
        }

        Elements wordSections = dictionary.select("div.pr.entry-body__el");
        if (wordSections.isEmpty()) {
            log.warn("Not Found main section for word: {}", word);
            return results;
        }

        for (Element entry : wordSections) {
            Element headerSection = entry.selectFirst("div.pos-header.dpos-h");
            if (headerSection == null) {
                continue;
            }

            Element wordForm = headerSection.selectFirst("span.hw.dhw");
            String extractedWord = (wordForm != null) ? wordForm.text() : word;

            Element posEl = headerSection.selectFirst("span.pos.dpos");
            String pos = (posEl != null) ? posEl.text() : null;

            Elements overallGramaLabels = headerSection.select("span.gc.dgc");
            List<String> overallGrammarList = overallGramaLabels.stream().map(Element::text).toList();

            Elements ipaUkEl = headerSection.select("span.uk span.pron.dpron");
            String ipaUk =  String.join(" " , ipaUkEl.stream().map(Element::text).toList());

            Elements ipaUsEl = headerSection.select("span.us span.pron.dpron");
            String ipaUs =  String.join(" " , ipaUsEl.stream().map(Element::text).toList());

            Element audioUkEl = headerSection.selectFirst("span.uk source[type='audio/mpeg']");
            String audioUk = (audioUkEl != null) ? audioUkEl.attr("src") : null;

            Element audioUsEl = headerSection.selectFirst("span.us source[type='audio/mpeg']");
            String audioUs = (audioUsEl != null) ? audioUsEl.attr("src") : null;

            // Inflections
            Inflect inflect = parserInflection(entry, pos);

            // Senses
            Elements meaningEls = entry.select("div.pr.dsense");
            List<WordSense> wordSenses = new ArrayList<>();


            for (Element meaning : meaningEls) {
                Element guideWordEl = meaning.selectFirst("span.guideword.dsense_gw");
                String guideWord = (guideWordEl != null) ? guideWordEl.text() : null;

                Elements phraseBlocks = meaning.select("div.pr.phrase-block.dphrase-block");
                for (Element block : phraseBlocks) {
                    Element phraseEl = block.selectFirst("div.phrase-head.dphrase_h");
                    String phraseWord = (phraseEl != null) ? phraseEl.text() : null;
                    wordSenses.add(parseWordSense(block, guideWord, phraseWord, overallGrammarList));
                }

                Elements blocks = meaning.select("div.def-block.ddef_block");
                for (Element block : blocks) {
                    wordSenses.add(parseWordSense(block, guideWord, null, overallGrammarList));
                }
            }
            PendingQueueEntry pendingQueueEntry = PendingQueueEntry.builder()
                    .id(UUID.randomUUID())
                    .status(WordStatus.WAITING)
                    .word(extractedWord)
                    .lemma(word)
                    .dictionary("Cambridge")
                    .pos(pos)
                    .ipaUk(ipaUk)
                    .ipaUs(ipaUs)
                    .audioUrlUk(audioUk)
                    .audioUrlUs(audioUs)
                    .wordSenses(wordSenses)
                    .plural(inflect.getPlural())
                    .past(inflect.getPast())
                    .pastParticiple(inflect.getPastParticiple())
                    .presentParticiple(inflect.getPresentParticiple())
                    .superlative(inflect.getSuperlative())
                    .comparative(inflect.getComparative())
                    .createdAt(LocalDate.now())
                    .build();

            results.add(pendingQueueEntry);
        }

        return results;
    }

    private WordSense parseWordSense(Element block, String guideWord, String phraseWord, List<String> defaultGrammarList) {
        Element cefrEl = block.selectFirst("span.epp-xref");
        String cefr = (cefrEl != null) ? cefrEl.text() : null;

        Element defEl = block.selectFirst("div.def.ddef_d");
        String definition = (defEl != null) ? defEl.text() : null;

        Elements exampleEls = block.select("span.eg.deg");
        List<String> examples = exampleEls.stream().map(Element::text).toList();

        Elements gramaLabels = block.select("span.def-info.ddef-info span.gc.dgc");
        List<String> grammarList = gramaLabels.stream().map(Element::text).toList();
        if (grammarList.isEmpty()) {
            grammarList = defaultGrammarList;
        }

        Elements collocationEls = block.select("span.lu.dlu");
        List<Collocation> collocations = collocationEls.stream()
                .map(el -> Collocation.builder().phrase(el.text()).build())
                .toList();

        return WordSense.builder()
                .id(UUID.randomUUID())
                .phrase(phraseWord)
                .guideWord(guideWord)
                .cefr(cefr)
                .definition(definition)
                .examples(examples)
                .grammarLabels(grammarList)
                .collocations(collocations)
                .build();
    }

    private Inflect parserInflection(Element mainContent, String pos) {
        Elements blocks = mainContent.select("span.irreg-infls.dinfls");
        Inflect infObj = Inflect.builder().build();
        Set<String> inflectionsSet = new LinkedHashSet<>();

        for (Element el : blocks) {
            Elements pairs = el.select("span.inf-group.dinfg");
            for (Element pr : pairs) {
                Element labelEl = pr.selectFirst("span.lab.dlab");
                Element valueEl = pr.selectFirst("b.inf.dinf");

                String key = (labelEl != null) ? labelEl.text() : null;
                String value = (valueEl != null) ? valueEl.text() : null;

                if (value != null) {
                    if (key != null && setInflection(infObj, key, value)) {
                        continue;
                    }
                    inflectionsSet.add(value);
                }
            }
        }

        List<String> inflections = new ArrayList<>(inflectionsSet);

        if (("verb".equalsIgnoreCase(pos) || "auxiliary verb".equalsIgnoreCase(pos)) && !inflections.isEmpty()) {
            int startIndex = 0, endIndex = inflections.size();
            if (inflections.getFirst().contains("ing")) {
                startIndex++;
                infObj.setPresentParticiple(inflections.getFirst());
            }
            int remain = endIndex - startIndex;
            if (remain > 1) {
                infObj.setPast(String.join("|", inflections.subList(startIndex, endIndex - 1)));
                infObj.setPastParticiple(inflections.getLast());
            } else if (remain == 1) {
                String currWord = inflections.get(startIndex);
                infObj.setPast(currWord);
                infObj.setPastParticiple(currWord);
            }
        }
        if ("noun".equalsIgnoreCase(pos) && !inflections.isEmpty()) {
            infObj.setPlural(String.join("|", inflections));
        }
        if (("adjective".equalsIgnoreCase(pos) || "adverb".equalsIgnoreCase(pos)) && !inflections.isEmpty()) {
            infObj.setComparative(inflections.getFirst());
            if (inflections.size() > 1) {
                infObj.setSuperlative(String.join("|", inflections.subList(1, inflections.size())));
            }
        }
        return infObj;
    }

    private boolean setInflection(Inflect inflect, String key, String value) {
        String lowerKey = key.toLowerCase();
        if (lowerKey.contains("present participle")) {
            inflect.setPresentParticiple(value);
            return true;
        } else if (lowerKey.contains("past tense") || lowerKey.equals("past")) {
            inflect.setPast(value);
            return true;
        } else if (lowerKey.contains("past participle")) {
            inflect.setPastParticiple(value);
            return true;
        } else if (lowerKey.contains("plural")) {
            inflect.setPlural(value);
            return true;
        } else if (lowerKey.contains("comparative")) {
            inflect.setComparative(value);
            return true;
        } else if (lowerKey.contains("superlative")) {
            inflect.setSuperlative(value);
            return true;
        }
        return false;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Inflect {
        private String plural;
        private String past;
        private String pastParticiple;
        private String presentParticiple;
        private String superlative;
        private String comparative;
    }

    public static void main(String[] args) {
        ProxyManager dummyProxyManager = null;
        CambridgeProvider provider = new CambridgeProvider(dummyProxyManager);
        provider.cambridgeBaseUrl = "https://dictionary.cambridge.org/dictionary/english/";

        List<PendingQueueEntry> entries = provider.crawlCambridge("have");
        List<String> entriesJSon= entries.stream().map(JsonUtils::toJson).toList();
        System.out.println("Result entries size: " + entries.size());
        for (PendingQueueEntry e : entries) {
            System.out.println(e);
        }
    }
}


