package com.shaswat;

import com.shaswat.index.IndexBuilder;
import com.shaswat.query.Query;
import com.shaswat.query.QueryParser;
import com.shaswat.search.SearchEngine;
import com.shaswat.search.SearchResult;
import com.shaswat.search.SnippetGenerator;
import com.shaswat.store.DocStore;
import com.shaswat.ingest.Document;

import java.io.File;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {

        // ---- Files ----
        String outputDir = "output";
        String outputJsonl = outputDir + "/wiki_docs.jsonl";

        new File(outputDir).mkdirs();

        // ---- Build index from JSONL ----
        IndexBuilder builder = new IndexBuilder();
        IndexBuilder.BuildResult result = builder.buildFromJsonl(outputJsonl, 5000);

        System.out.println("Docs indexed: " + result.docsIndexed);
        System.out.println("Title vocabulary size: " + result.titleIndex.vocabularySize());
        System.out.println("Body vocabulary size: " + result.bodyIndex.vocabularySize());

        // ---- Load docs into memory (DocStore) ----
        DocStore store = new DocStore();
        store.loadFromJsonl(outputJsonl);

        // ---- Create search components ----
        QueryParser qp = new QueryParser();
        SearchEngine se = new SearchEngine(
        result.titleIndex,
        result.bodyIndex,
        result.docsIndexed,
        result.titleLen,
        result.bodyLen,
        result.avgTitleLen,
        result.avgBodyLen
);

        SnippetGenerator sg = new SnippetGenerator();

        System.out.println("\nWiki-Search CLI ready.");
System.out.println("Type a query and press Enter.");
System.out.println("Examples:");
System.out.println("  binary search");
System.out.println("  \"machine learning\"");
System.out.println("  binary OR learning");
System.out.println("Type 'exit' to quit.\n");

java.util.Scanner sc = new java.util.Scanner(System.in);

while (true) {
    System.out.print("> ");
    String rawQuery = sc.nextLine();

    if (rawQuery == null) continue;
    rawQuery = rawQuery.trim();

    if (rawQuery.equalsIgnoreCase("exit")) {
        System.out.println("Bye.");
        break;
    }

    if (rawQuery.isBlank()) {
        continue;
    }

    runAndPrint(se, qp, sg, store, rawQuery);
}

    }

    private static void runAndPrint(
            SearchEngine se,
            QueryParser qp,
            SnippetGenerator sg,
            DocStore store,
            String rawQuery
    ) {
        Query q = qp.parse(rawQuery);

        System.out.println("\n==============================");
        System.out.println("QUERY: " + rawQuery);

        List<SearchResult> results = se.searchRanked(q, 10);

        if (results.isEmpty()) {
            System.out.println("No results.");
            return;
        }

        for (int i = 0; i < results.size(); i++) {
            SearchResult r = results.get(i);
            Document doc = store.get(r.getDocId());

            String snippet = sg.makeSnippet(doc.getBody(), q.getKeywords(), 160);

            System.out.println((i + 1) + ") " + doc.getTitle() + "   score=" + r.getScore());
            System.out.println("   " + snippet);
        }
    }
}
