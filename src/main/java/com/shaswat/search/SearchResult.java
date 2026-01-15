package com.shaswat.search;

public class SearchResult implements Comparable<SearchResult> {
    private final int docId;
    private final double score;

    public SearchResult(int docId, double score) {
        this.docId = docId;
        this.score = score;
    }

    public int getDocId() {
        return docId;
    }

    public double getScore() {
        return score;
    }

    @Override
    public int compareTo(SearchResult other) {
        return Double.compare(other.score, this.score); // descending
    }
}
