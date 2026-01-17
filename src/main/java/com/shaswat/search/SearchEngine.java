package com.shaswat.search;

import com.shaswat.index.InvertedIndex;
import com.shaswat.index.Posting;
import com.shaswat.query.Query;

import java.util.*;

public class SearchEngine {

    private final InvertedIndex titleIndex;
    private final InvertedIndex bodyIndex;

    private final int N;
    private final int[] titleLen;
    private final int[] bodyLen;
    private final double avgTitleLen;
    private final double avgBodyLen;

    private final PhraseMatcher phraseMatcher = new PhraseMatcher();

    public SearchEngine(InvertedIndex titleIndex,
                        InvertedIndex bodyIndex,
                        int N,
                        int[] titleLen,
                        int[] bodyLen,
                        double avgTitleLen,
                        double avgBodyLen) {

        this.titleIndex = titleIndex;
        this.bodyIndex = bodyIndex;

        this.N = N;
        this.titleLen = titleLen;
        this.bodyLen = bodyLen;
        this.avgTitleLen = avgTitleLen;
        this.avgBodyLen = avgBodyLen;
    }

    public List<SearchResult> searchRanked(Query q, int topK) {
        Set<Integer> docs = search(q);

        BM25Scorer bm25 = new BM25Scorer(1.2, 0.75);
        List<SearchResult> results = new ArrayList<>();

        // phrase docs (explicit quotes) - computed ONCE per query
        Set<Integer> titlePhraseDocs = Collections.emptySet();
        Set<Integer> bodyPhraseDocs = Collections.emptySet();

        if (q.hasPhrase()) {
            titlePhraseDocs = phraseMatcher.findPhraseMatches(titleIndex, q.getPhraseTokens());
            bodyPhraseDocs = phraseMatcher.findPhraseMatches(bodyIndex, q.getPhraseTokens());
        }

        // auto phrase docs (no quotes, multi-word query) - computed ONCE per query
        Set<Integer> titleAutoPhraseDocs = Collections.emptySet();
        Set<Integer> bodyAutoPhraseDocs = Collections.emptySet();

        if (!q.hasPhrase() && q.getKeywords().size() >= 2) {
            titleAutoPhraseDocs = phraseMatcher.findPhraseMatches(titleIndex, q.getKeywords());
            bodyAutoPhraseDocs = phraseMatcher.findPhraseMatches(bodyIndex, q.getKeywords());
        }

        for (int docId : docs) {
            double score = 0.0;

            // BM25 keyword scoring
            for (String term : q.getKeywords()) {
                score += 1.8 * bm25.scoreTerm(term, docId, titleIndex, N, titleLen[docId], avgTitleLen);
                score += 1.0 * bm25.scoreTerm(term, docId, bodyIndex, N, bodyLen[docId], avgBodyLen);
            }

            // Auto phrase boost (multi-word query but no quotes)
            if (!q.hasPhrase() && q.getKeywords().size() >= 2) {
                if (titleAutoPhraseDocs.contains(docId)) score += 6.0;
                if (bodyAutoPhraseDocs.contains(docId)) score += 3.0;
            }

            // Explicit phrase bonus (query used quotes)
            if (q.hasPhrase()) {
                if (titlePhraseDocs.contains(docId)) score += 5.0;
                if (bodyPhraseDocs.contains(docId)) score += 2.0;
            }

            results.add(new SearchResult(docId, score));
        }

        Collections.sort(results);
        if (results.size() > topK) return results.subList(0, topK);
        return results;
    }

    public Set<Integer> search(Query q) {
        Set<Integer> candidateDocs;

        // 1) handle keyword part
        if (!q.getKeywords().isEmpty()) {
            if (q.isOrQuery()) {
                candidateDocs = unionDocsForTerms(q.getKeywords());
            } else {
                candidateDocs = intersectDocsForTerms(q.getKeywords());
            }
        } else {
            candidateDocs = new HashSet<>(); // will be replaced by phrase docs if phrase exists
        }

        // 2) handle phrase part
        if (q.hasPhrase()) {
            Set<Integer> phraseDocs = new HashSet<>();
            phraseDocs.addAll(phraseMatcher.findPhraseMatches(titleIndex, q.getPhraseTokens()));
            phraseDocs.addAll(phraseMatcher.findPhraseMatches(bodyIndex, q.getPhraseTokens()));

            // If query is ONLY a phrase, return phrase results
            if (q.getKeywords().isEmpty()) {
                return phraseDocs;
            }

            // Mixed query => phrase must match AND keyword conditions must match
            candidateDocs.retainAll(phraseDocs);
        }

        return candidateDocs;
    }

    private Set<Integer> intersectDocsForTerms(List<String> terms) {
        Set<Integer> result = null;

        for (String term : terms) {
            Set<Integer> termDocs = docsForTerm(term);

            if (result == null) {
                result = new HashSet<>(termDocs);
            } else {
                result.retainAll(termDocs);
            }

            if (result.isEmpty()) break;
        }

        return (result == null) ? Collections.emptySet() : result;
    }

    private Set<Integer> unionDocsForTerms(List<String> terms) {
        Set<Integer> result = new HashSet<>();

        for (String term : terms) {
            result.addAll(docsForTerm(term));
        }

        return result;
    }

    private Set<Integer> docsForTerm(String term) {
        Set<Integer> docs = new HashSet<>();

        for (Posting p : titleIndex.getPostings(term)) docs.add(p.getDocId());
        for (Posting p : bodyIndex.getPostings(term)) docs.add(p.getDocId());

        return docs;
    }
}
