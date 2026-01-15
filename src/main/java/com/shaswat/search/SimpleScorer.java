package com.shaswat.search;

import com.shaswat.index.InvertedIndex;
import com.shaswat.query.Query;

import java.util.HashSet;
import java.util.Set;

public class SimpleScorer {

    private final PhraseMatcher phraseMatcher = new PhraseMatcher();

    public double scoreDoc(int docId, Query q, InvertedIndex titleIndex, InvertedIndex bodyIndex) {
        double score = 0.0;

        // keyword scoring
        for (String term : q.getKeywords()) {
            if (containsDoc(titleIndex, term, docId)) score += 5;  // title boost
            if (containsDoc(bodyIndex, term, docId)) score += 2;   // body match
        }

        // phrase bonus scoring
        if (q.hasPhrase()) {
            Set<Integer> titlePhrase = phraseMatcher.findPhraseMatches(titleIndex, q.getPhraseTokens());
            Set<Integer> bodyPhrase = phraseMatcher.findPhraseMatches(bodyIndex, q.getPhraseTokens());

            if (titlePhrase.contains(docId)) score += 25;
            if (bodyPhrase.contains(docId)) score += 10;
        }

        return score;
    }

    private boolean containsDoc(InvertedIndex index, String term, int docId) {
        return index.getPostings(term).stream().anyMatch(p -> p.getDocId() == docId);
    }
}
