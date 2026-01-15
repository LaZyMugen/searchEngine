package com.shaswat.search;

import com.shaswat.index.InvertedIndex;
import com.shaswat.index.Posting;

import java.util.*;

public class PhraseMatcher {

    /**
     * Returns docIds where the exact phrase occurs.
     * Phrase tokens must already be normalized (lowercase, tokenized).
     */
    public Set<Integer> findPhraseMatches(InvertedIndex index, List<String> phraseTokens) {
        if (phraseTokens == null || phraseTokens.isEmpty()) return Collections.emptySet();

        // If any term missing completely -> no matches
        for (String term : phraseTokens) {
            if (!index.containsTerm(term)) {
                return Collections.emptySet();
            }
        }

        // Candidate docs = intersection of docs containing all terms
        Map<Integer, List<Integer>> firstTermDocs = postingsToDocPositionsMap(index.getPostings(phraseTokens.get(0)));

        Set<Integer> candidates = new HashSet<>(firstTermDocs.keySet());

        for (int i = 1; i < phraseTokens.size(); i++) {
            Map<Integer, List<Integer>> termDocs = postingsToDocPositionsMap(index.getPostings(phraseTokens.get(i)));
            candidates.retainAll(termDocs.keySet());
            if (candidates.isEmpty()) return Collections.emptySet();
        }

        // Now verify adjacency using positions
        Set<Integer> matches = new HashSet<>();
        for (int docId : candidates) {
            if (phraseExistsInDoc(index, phraseTokens, docId)) {
                matches.add(docId);
            }
        }

        return matches;
    }

    private boolean phraseExistsInDoc(InvertedIndex index, List<String> phraseTokens, int docId) {
        // positions of first term
        List<Integer> basePositions = positionsForDoc(index, phraseTokens.get(0), docId);
        if (basePositions.isEmpty()) return false;

        // Use a set for fast lookup for each next term
        for (int basePos : basePositions) {
            boolean ok = true;

            for (int offset = 1; offset < phraseTokens.size(); offset++) {
                String term = phraseTokens.get(offset);
                List<Integer> positions = positionsForDoc(index, term, docId);

                // check if basePos + offset exists in this term positions
                if (!containsPosition(positions, basePos + offset)) {
                    ok = false;
                    break;
                }
            }

            if (ok) return true;
        }

        return false;
    }

    private boolean containsPosition(List<Integer> sortedPositions, int target) {
        // positions are added in increasing order during indexing, so binary search works
        return Collections.binarySearch(sortedPositions, target) >= 0;
    }

    private List<Integer> positionsForDoc(InvertedIndex index, String term, int docId) {
        for (Posting p : index.getPostings(term)) {
            if (p.getDocId() == docId) return p.getPositions();
        }
        return Collections.emptyList();
    }

    private Map<Integer, List<Integer>> postingsToDocPositionsMap(List<Posting> postings) {
        Map<Integer, List<Integer>> map = new HashMap<>();
        for (Posting p : postings) {
            map.put(p.getDocId(), p.getPositions());
        }
        return map;
    }
}
