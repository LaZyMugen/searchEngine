package com.shaswat.tools;

import com.shaswat.ingest.WikiDumpParser;

public class GenerateJsonl {

    public static void main(String[] args) throws Exception {

        // Defaults (work for your machine)
        String dumpPath = "D:/devProjs/data/wiki/simplewiki-latest-pages-articles.xml.bz2";
        String outputJsonl = "output/wiki_docs.jsonl";
        int maxDocs = 50000;

        // Optional CLI args:
        // args[0] = dumpPath
        // args[1] = outputJsonl
        // args[2] = maxDocs
        if (args.length >= 1) dumpPath = args[0];
        if (args.length >= 2) outputJsonl = args[1];
        if (args.length >= 3) maxDocs = Integer.parseInt(args[2]);

        System.out.println("Dump path : " + dumpPath);
        System.out.println("Output    : " + outputJsonl);
        System.out.println("Max docs  : " + maxDocs);

        long t0 = System.currentTimeMillis();

        WikiDumpParser parser = new WikiDumpParser();
        int written = parser.parseToJsonl(dumpPath, outputJsonl, maxDocs);

        long t1 = System.currentTimeMillis();

        System.out.println("Generated JSONL docs: " + written);
        System.out.println("Time taken: " + (t1 - t0) + " ms");
    }
}
