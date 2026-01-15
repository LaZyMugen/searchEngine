package com.shaswat.index;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shaswat.ingest.Document;
import com.shaswat.preprocess.Tokenizer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class IndexBuilder {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Tokenizer tokenizer = new Tokenizer();

    public static class BuildResult {
    public final InvertedIndex titleIndex;
    public final InvertedIndex bodyIndex;
    public final int docsIndexed;

    public final int[] titleLen;
    public final int[] bodyLen;

    public final double avgTitleLen;
    public final double avgBodyLen;

    public BuildResult(InvertedIndex titleIndex,
                       InvertedIndex bodyIndex,
                       int docsIndexed,
                       int[] titleLen,
                       int[] bodyLen,
                       double avgTitleLen,
                       double avgBodyLen) {
        this.titleIndex = titleIndex;
        this.bodyIndex = bodyIndex;
        this.docsIndexed = docsIndexed;
        this.titleLen = titleLen;
        this.bodyLen = bodyLen;
        this.avgTitleLen = avgTitleLen;
        this.avgBodyLen = avgBodyLen;
    }
}


    public BuildResult buildFromJsonl(String jsonlPath, int maxDocs) throws Exception {
    InvertedIndex titleIndex = new InvertedIndex();
    InvertedIndex bodyIndex = new InvertedIndex();

    int[] titleLen = new int[maxDocs];
    int[] bodyLen = new int[maxDocs];

    int count = 0;
    long totalTitleTokens = 0;
    long totalBodyTokens = 0;

    try (BufferedReader br = new BufferedReader(new FileReader(jsonlPath, StandardCharsets.UTF_8))) {
        String line;

        while ((line = br.readLine()) != null) {
            if (line.isBlank()) continue;

            Document doc = mapper.readValue(line, Document.class);

            int docId = doc.getDocId();

            int tLen = indexField(titleIndex, docId, doc.getTitle());
            int bLen = indexField(bodyIndex, docId, doc.getBody());

            titleLen[docId] = tLen;
            bodyLen[docId] = bLen;

            totalTitleTokens += tLen;
            totalBodyTokens += bLen;

            count++;
            if (count >= maxDocs) break;
        }
    }

    double avgTitleLen = (count == 0) ? 0.0 : (double) totalTitleTokens / count;
    double avgBodyLen = (count == 0) ? 0.0 : (double) totalBodyTokens / count;

    return new BuildResult(titleIndex, bodyIndex, count, titleLen, bodyLen, avgTitleLen, avgBodyLen);
}


    private int indexField(InvertedIndex index, int docId, String text) {
    List<String> tokens = tokenizer.tokenize(text);

    for (int pos = 0; pos < tokens.size(); pos++) {
        String term = tokens.get(pos);
        index.addTermOccurrence(term, docId, pos);
    }

    return tokens.size();
}

}
