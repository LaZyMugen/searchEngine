package com.shaswat.tools;

public class GenerateJsonl {
    
}
package com.shaswat.tools;

import com.shaswat.ingest.WikiDumpParser;

public class GenerateJsonl {
    public static void main(String[] args) throws Exception {

        String dumpPath = "D:/devProjs/data/wiki/simplewiki-latest-pages-articles.xml.bz2";
        String outputJsonl = "output/wiki_docs.jsonl";
        int maxDocs = 50000;

        WikiDumpParser parser = new WikiDumpParser();
        int written = parser.parseToJsonl(dumpPath, outputJsonl, maxDocs);

        System.out.println("Generated JSONL docs: " + written);
        System.out.println("Output: " + outputJsonl);
    }
}
