package com.shaswat.search;

import com.shaswat.index.InvertedIndex;
import com.shaswat.index.Posting;
import com.shaswat.query.Query;
import com.shaswat.search.BM25Scorer;
import com.shaswat.search.SearchResult;


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


 public java.util.List<SearchResult> searchRanked(com.shaswat.query.Query q, int topK) {
    Set<Integer> docs = search(q);

    BM25Scorer bm25 = new BM25Scorer(1.2, 0.75);

    java.util.List<SearchResult> results = new java.util.ArrayList<>();

    java.util.Set<Integer> titlePhraseDocs = java.util.Collections.emptySet();
java.util.Set<Integer> bodyPhraseDocs = java.util.Collections.emptySet();

if (q.hasPhrase()) {
    titlePhraseDocs = phraseMatcher.findPhraseMatches(titleIndex, q.getPhraseTokens());
    bodyPhraseDocs = phraseMatcher.findPhraseMatches(bodyIndex, q.getPhraseTokens());
}


    for (int docId : docs) {
        double score = 0.0;

        // BM25 keyword scoring
        for (String term : q.getKeywords()) {
            score += 1.8 * bm25.scoreTerm(term, docId, titleIndex, N, titleLen[docId], avgTitleLen);
            score += 1.0 * bm25.scoreTerm(term, docId, bodyIndex, N, bodyLen[docId], avgBodyLen);
        }

        // phrase bonus
        // phrase bonus (optimized: computed once per query)
if (q.hasPhrase()) {
    if (titlePhraseDocs.contains(docId)) score += 5.0;
    if (bodyPhraseDocs.contains(docId)) score += 2.0;
}

        results.add(new SearchResult(docId, score));
    }

    java.util.Collections.sort(results);
    if (results.size() > topK) return results.subList(0, topK);
    return results;
}



    public Set<Integer> search(Query q) {
        Set<Integer> candidateDocs = null;

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
