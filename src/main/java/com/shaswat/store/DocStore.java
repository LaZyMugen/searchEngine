package com.shaswat.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shaswat.ingest.Document;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class DocStore {

    private final Map<Integer, Document> docsById = new HashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    public void loadFromJsonl(String jsonlPath) throws Exception {
        docsById.clear();

        try (BufferedReader br = new BufferedReader(new FileReader(jsonlPath, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                Document doc = mapper.readValue(line, Document.class);
                docsById.put(doc.getDocId(), doc);
            }
        }
    }

    public Document get(int docId) {
        return docsById.get(docId);
    }
}
