package com.shaswat.index;

import java.util.*;

public class InvertedIndex {

    // term -> (docId -> Posting)
    private final Map<String, Map<Integer, Posting>> index = new HashMap<>();

    public void addTermOccurrence(String term, int docId, int position) {
        index
            .computeIfAbsent(term, t -> new HashMap<>())
            .computeIfAbsent(docId, id -> new Posting(docId))
            .addPosition(position);
    }

    public int getTf(String term, int docId) {
    Map<Integer, Posting> byDoc = index.get(term);
    if (byDoc == null) return 0;

    Posting p = byDoc.get(docId);
    if (p == null) return 0;

    return p.getPositions().size();
}

    public int getDf(String term) {
    Map<Integer, Posting> byDoc = index.get(term);
    if (byDoc == null) return 0;
    return byDoc.size();
}



    /**
     * For query-time usage we often want term -> List<Posting>.
     * This converts docId maps into postings list.
     */
    public List<Posting> getPostings(String term) {
        Map<Integer, Posting> byDoc = index.get(term);
        if (byDoc == null) return Collections.emptyList();

        // Sort by docId for consistent behavior (also helps intersections later)
        List<Posting> postings = new ArrayList<>(byDoc.values());
        postings.sort(Comparator.comparingInt(Posting::getDocId));
        return postings;
    }

    public int vocabularySize() {
        return index.size();
    }

    public boolean containsTerm(String term) {
        return index.containsKey(term);
    }
}
