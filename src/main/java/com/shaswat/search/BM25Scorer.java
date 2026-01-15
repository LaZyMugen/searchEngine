package com.shaswat.search;

import com.shaswat.index.InvertedIndex;
import com.shaswat.index.Posting;

import java.util.List;

public class BM25Scorer {

    // Standard BM25 params
    private final double k1;
    private final double b;

    public BM25Scorer(double k1, double b) {
        this.k1 = k1;
        this.b = b;
    }

    public double scoreTerm(String term,
                        int docId,
                        InvertedIndex index,
                        int N,
                        int docLen,
                        double avgDocLen) {

    int df = index.getDf(term);
    if (df == 0) return 0.0;

    int tf = index.getTf(term, docId);
    if (tf == 0) return 0.0;

    double idf = Math.log(1.0 + (N - df + 0.5) / (df + 0.5));

    double norm = (1.0 - b) + b * (docLen / avgDocLen);
    double denom = tf + k1 * norm;
    double numer = tf * (k1 + 1.0);

    return idf * (numer / denom);
}

}
