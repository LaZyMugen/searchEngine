package com.shaswat;

import com.shaswat.index.*;
import com.shaswat.ingest.Document;
import com.shaswat.query.Query;
import com.shaswat.query.QueryParser;
import com.shaswat.search.SearchEngine;
import com.shaswat.search.SearchResult;
import com.shaswat.search.SnippetGenerator;
import com.shaswat.store.DocStore;

import java.io.File;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws Exception {

        // ---- Files ----
        String outputDir = "output";
        String outputJsonl = outputDir + "/wiki_docs.jsonl";

        // ---- ONE-TIME: Generate wiki_docs.jsonl from real Wikipedia dump ----
String dumpPath = "D:/devProjs/data/wiki/simplewiki-latest-pages-articles.xml.bz2";


int maxDocsForJsonl = 50000;

com.shaswat.ingest.WikiDumpParser parser = new com.shaswat.ingest.WikiDumpParser();
int written = parser.parseToJsonl(dumpPath, outputJsonl, maxDocsForJsonl);

System.out.println("Generated JSONL docs: " + written);


        String titleIndexPath = outputDir + "/title_index.txt";
        String bodyIndexPath  = outputDir + "/body_index.txt";
        String metaPath       = outputDir + "/index_meta.json";

        new File(outputDir).mkdirs();

        // ---- Load docs into memory (DocStore) ----
        DocStore store = new DocStore();
        store.loadFromJsonl(outputJsonl);

        // ---- Decide: load index or build index ----
        IndexReader indexReader = new IndexReader();
        MetadataIO mio = new MetadataIO();

        InvertedIndex titleIndex;
        InvertedIndex bodyIndex;
        IndexMetadata meta;

        boolean canLoad = filesExist(titleIndexPath, bodyIndexPath, metaPath);
        int maxDocs = 50000;


if (canLoad) {
    System.out.println("Found existing index files. Loading from disk...");

    long tLoad0 = System.currentTimeMillis();

    titleIndex = indexReader.readFromFile(titleIndexPath);
    bodyIndex  = indexReader.readFromFile(bodyIndexPath);
    meta       = mio.read(metaPath);

    long tLoad1 = System.currentTimeMillis();

    System.out.println("Loaded index + metadata successfully.");
    System.out.println("Title vocab size: " + titleIndex.vocabularySize());
    System.out.println("Body vocab size: " + bodyIndex.vocabularySize());
    System.out.println("N=" + meta.N + ", avgTitleLen=" + meta.avgTitleLen + ", avgBodyLen=" + meta.avgBodyLen);
    System.out.println("Load time: " + (tLoad1 - tLoad0) + " ms");

} else {
    System.out.println("Index files not found. Building index from JSONL...");
    System.out.println("Building index for maxDocs=" + maxDocs);

    long tBuild0 = System.currentTimeMillis();

    IndexBuilder builder = new IndexBuilder();
    IndexBuilder.BuildResult result = builder.buildFromJsonl(outputJsonl, maxDocs);

    long tBuild1 = System.currentTimeMillis();

    System.out.println("Docs indexed: " + result.docsIndexed);
    System.out.println("Title vocabulary size: " + result.titleIndex.vocabularySize());
    System.out.println("Body vocabulary size: " + result.bodyIndex.vocabularySize());
    System.out.println("Index build time: " + (tBuild1 - tBuild0) + " ms");

    // Save metadata
    meta = new IndexMetadata(
            result.docsIndexed,
            result.avgTitleLen,
            result.avgBodyLen,
            result.titleLen,
            result.bodyLen
    );

    long tWrite0 = System.currentTimeMillis();

    mio.write(metaPath, meta);

    IndexWriter writer = new IndexWriter();
    writer.writeToFile(result.titleIndex, titleIndexPath);
    writer.writeToFile(result.bodyIndex, bodyIndexPath);

    long tWrite1 = System.currentTimeMillis();

    System.out.println("Disk write time: " + (tWrite1 - tWrite0) + " ms");
    System.out.println("Index + metadata saved to disk.");

    // Use in-memory indexes now
    titleIndex = result.titleIndex;
    bodyIndex  = result.bodyIndex;
}


        // ---- Create search components ----
        QueryParser qp = new QueryParser();
        SearchEngine se = new SearchEngine(
                titleIndex,
                bodyIndex,
                meta.N,
                meta.titleLen,
                meta.bodyLen,
                meta.avgTitleLen,
                meta.avgBodyLen
        );

        SnippetGenerator sg = new SnippetGenerator();

        // ---- CLI loop ----
        System.out.println("\nWiki-Search CLI ready.");
        System.out.println("Type a query and press Enter.");
        System.out.println("Examples:");
        System.out.println("  binary search");
        System.out.println("  \"machine learning\"");
        System.out.println("  binary OR learning");
        System.out.println("Type 'exit' to quit.\n");

        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.print("> ");
            String rawQuery = sc.nextLine();

            if (rawQuery == null) continue;
            rawQuery = rawQuery.trim();

            if (rawQuery.equalsIgnoreCase("exit")) {
                System.out.println("Bye.");
                break;
            }

            if (rawQuery.isBlank()) continue;

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
        long t0 = System.currentTimeMillis();


        List<SearchResult> results = se.searchRanked(q, 10);
        long t1 = System.currentTimeMillis();
System.out.println("Query time: " + (t1 - t0) + " ms");


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

    private static boolean filesExist(String... paths) {
        for (String p : paths) {
            if (!new File(p).exists()) return false;
        }
        return true;
    }
}
