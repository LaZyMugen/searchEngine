package com.shaswat.index;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;

public class IndexReader {

    public InvertedIndex readFromFile(String path) throws Exception {
        InvertedIndex idx = new InvertedIndex();

        try (BufferedReader br = new BufferedReader(new FileReader(path, StandardCharsets.UTF_8))) {
            String line;

            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;

                // term|docId:pos,pos docId:pos,pos
                String[] parts = line.split("\\|", 2);
                if (parts.length != 2) continue;

                String term = parts[0];
                String postingsPart = parts[1].trim();

                if (postingsPart.isEmpty()) continue;

                String[] postings = postingsPart.split(" ");
                for (String posting : postings) {
                    // docId:pos,pos
                    String[] dp = posting.split(":");
                    int docId = Integer.parseInt(dp[0]);

                    if (dp.length == 1) continue; // no positions??

                    String[] posArr = dp[1].split(",");
                    for (String posStr : posArr) {
                        if (posStr.isBlank()) continue;
                        int position = Integer.parseInt(posStr);
                        idx.addTermOccurrence(term, docId, position);
                    }
                }
            }
        }

        return idx;
    }
}
