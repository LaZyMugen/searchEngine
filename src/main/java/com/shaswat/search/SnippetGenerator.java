package com.shaswat.search;

import java.util.List;

public class SnippetGenerator {

    public String makeSnippet(String text, List<String> queryTerms, int maxLen) {
        if (text == null) return "";
        String clean = text.replaceAll("\\s+", " ").trim();
        if (clean.length() <= maxLen) return clean;

        String lower = clean.toLowerCase();

        int bestPos = -1;
        for (String term : queryTerms) {
            int p = lower.indexOf(term.toLowerCase());
            if (p != -1 && (bestPos == -1 || p < bestPos)) {
                bestPos = p;
            }
        }

        if (bestPos == -1) {
            return clean.substring(0, maxLen) + "...";
        }

        int start = Math.max(0, bestPos - 40);
        int end = Math.min(clean.length(), start + maxLen);

        String snippet = clean.substring(start, end);
        if (start > 0) snippet = "..." + snippet;
        if (end < clean.length()) snippet = snippet + "...";
        return snippet;
    }
}
