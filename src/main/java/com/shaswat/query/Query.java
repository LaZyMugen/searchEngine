package com.shaswat.query;

import java.util.List;

public class Query {
    private final List<String> keywords;     // normal tokens (non-phrase)
    private final List<String> phraseTokens; // tokens inside "...", empty if none
    private final boolean isOrQuery;

    public Query(List<String> keywords, List<String> phraseTokens, boolean isOrQuery) {
        this.keywords = keywords;
        this.phraseTokens = phraseTokens;
        this.isOrQuery = isOrQuery;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public List<String> getPhraseTokens() {
        return phraseTokens;
    }

    public boolean isOrQuery() {
        return isOrQuery;
    }

    public boolean hasPhrase() {
        return phraseTokens != null && !phraseTokens.isEmpty();
    }
}
