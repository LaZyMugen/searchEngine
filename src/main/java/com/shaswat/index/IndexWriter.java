package com.shaswat.index;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class IndexWriter {

    public void writeToFile(InvertedIndex index, String path) throws Exception {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path, StandardCharsets.UTF_8))) {

            // Write terms in sorted order (easy debugging + consistent output)
            List<String> terms = new ArrayList<>(index.allTerms());
            Collections.sort(terms);

            for (String term : terms) {
                List<Posting> postings = index.getPostings(term);

                StringBuilder line = new StringBuilder();
                line.append(term).append("|");

                for (int i = 0; i < postings.size(); i++) {
                    Posting p = postings.get(i);

                    line.append(p.getDocId()).append(":");

                    List<Integer> pos = p.getPositions();
                    for (int j = 0; j < pos.size(); j++) {
                        line.append(pos.get(j));
                        if (j < pos.size() - 1) line.append(",");
                    }

                    if (i < postings.size() - 1) line.append(" ");
                }

                bw.write(line.toString());
                bw.newLine();
            }
        }
    }
}
