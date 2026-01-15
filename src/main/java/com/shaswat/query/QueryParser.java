package com.shaswat.query;

import com.shaswat.preprocess.Tokenizer;

import java.util.ArrayList;
import java.util.List;

public class QueryParser {

    private final Tokenizer tokenizer = new Tokenizer();

    public Query parse(String raw) {
        if (raw == null) raw = "";
        raw = raw.trim();

        boolean isOr = raw.contains(" OR ");

        // Extract phrase if present in quotes
        String phraseInsideQuotes = null;
        int firstQuote = raw.indexOf('"');
        int secondQuote = raw.indexOf('"', firstQuote + 1);

        String remaining = raw;

        if (firstQuote != -1 && secondQuote != -1 && secondQuote > firstQuote) {
            phraseInsideQuotes = raw.substring(firstQuote + 1, secondQuote);
            remaining = (raw.substring(0, firstQuote) + " " + raw.substring(secondQuote + 1)).trim();
        }

        List<String> phraseTokens = (phraseInsideQuotes == null)
                ? List.of()
                : tokenizer.tokenize(phraseInsideQuotes);

        // keywords from remaining query (remove OR tokens if present)
        String keywordPart = remaining.replace(" OR ", " ");
        List<String> keywords = tokenizer.tokenize(keywordPart);

        return new Query(keywords, phraseTokens, isOr);
    }
}
