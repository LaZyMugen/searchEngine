package com.shaswat.ingest;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;

public class WikiDumpParser {

    private final ObjectMapper mapper = new ObjectMapper();

    public int parseToJsonl(String inputXmlPath, String outputJsonlPath, int maxDocs) throws Exception {

        XMLInputFactory factory = XMLInputFactory.newInstance();

        // Security best practice: disable external entities + DTD
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);

        int docId = 0;

        try (var inputStream = openPossiblyCompressedInputStream(inputXmlPath);
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputJsonlPath, StandardCharsets.UTF_8))) {

            XMLStreamReader reader = null;

            try {
                reader = factory.createXMLStreamReader(inputStream);

                String currentTag = null;

                StringBuilder titleBuffer = null;
                Integer ns = null;
                StringBuilder textBuffer = null;
                boolean isRedirect = false;

                boolean insidePage = false;

                System.out.println("Starting parsing of: " + inputXmlPath);

                while (reader.hasNext()) {
                    int event = reader.next();

                    if (event == XMLStreamConstants.START_ELEMENT) {
                        currentTag = reader.getLocalName();

                        if ("page".equals(currentTag)) {
                            insidePage = true;

                            // reset page state
                            titleBuffer = new StringBuilder();
                            ns = null;
                            textBuffer = new StringBuilder();
                            isRedirect = false;

                        } else if (insidePage && "redirect".equals(currentTag)) {
                            isRedirect = true;
                        }

                    } else if (event == XMLStreamConstants.CHARACTERS) {
                        if (!insidePage) continue;

                        String value = reader.getText();

                        if ("title".equals(currentTag)) {
                            if (titleBuffer != null) titleBuffer.append(value);

                        } else if ("ns".equals(currentTag)) {
                            String cleaned = value.trim();
                            if (!cleaned.isEmpty()) ns = Integer.parseInt(cleaned);

                        } else if ("text".equals(currentTag)) {
                            if (textBuffer != null) textBuffer.append(value);
                        }

                    } else if (event == XMLStreamConstants.END_ELEMENT) {
                        String endTag = reader.getLocalName();
                        currentTag = null;

                        if ("page".equals(endTag)) {
                            insidePage = false;

                            boolean isMainNamespace = (ns != null && ns == 0);

                            String title = (titleBuffer != null) ? titleBuffer.toString() : "";
                            String text = (textBuffer != null) ? textBuffer.toString() : "";

                            boolean hasContent = !title.isBlank() && !text.isBlank();

                            if (isMainNamespace && !isRedirect && hasContent) {

                                // ✅ Clean wiki markup
                                String cleanedTitle = title.trim();
                                String cleanedBody = cleanWikiText(text);

                                if (!cleanedBody.isBlank()) {
                                    Document doc = new Document(docId, cleanedTitle, cleanedBody);

                                    writer.write(mapper.writeValueAsString(doc));
                                    writer.newLine();

                                    docId++;

                                    if (docId % 500 == 0) {
                                        System.out.println("Processed " + docId + " documents");
                                    }

                                    if (docId >= maxDocs) break;
                                }
                            }
                        }
                    }
                }

            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception e) {
                        System.err.println("Failed to close XMLStreamReader: " + e.getMessage());
                    }
                }
            }
        }

        return docId;
    }

    private java.io.InputStream openPossiblyCompressedInputStream(String path) throws Exception {
        java.io.InputStream in = new java.io.BufferedInputStream(new java.io.FileInputStream(path));

        if (path.endsWith(".bz2")) {
            return new org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream(in, true);
        }

        return in;
    }

    /**
     * Minimal but effective cleaning of Wikipedia markup.
     * Not perfect, but makes indexing + snippets far more usable.
     */
    private String cleanWikiText(String raw) {
        if (raw == null) return "";

        String s = raw;

        // Remove <ref>...</ref>
        s = s.replaceAll("(?s)<ref.*?>.*?</ref>", " ");

        // Remove any remaining HTML tags
        s = s.replaceAll("(?s)<[^>]+>", " ");

       // Remove templates {{...}} (best-effort, repeated passes for light nesting)
for (int i = 0; i < 5; i++) {
    String next = s.replaceAll("(?s)\\{\\{[^\\{\\}]*\\}\\}", " ");
    if (next.equals(s)) break;
    s = next;
}

// Remove wiki tables {| ... |} (best-effort)
s = s.replaceAll("(?s)\\{\\|.*?\\|\\}", " ");

// Remove leftover table/infobox pipes noise
s = s.replaceAll("(?m)^\\|.*$", " ");



        // Remove File / Image links
        s = s.replaceAll("(?i)\\[\\[(File|Image):.*?\\]\\]", " ");

        // Remove Category links
        s = s.replaceAll("(?i)\\[\\[Category:.*?\\]\\]", " ");

        // Convert wiki links:
        // [[Page|Text]] -> Text
        s = s.replaceAll("\\[\\[[^\\]|]+\\|([^\\]]+)\\]\\]", "$1");

        // [[Page]] -> Page
        s = s.replaceAll("\\[\\[([^\\]]+)\\]\\]", "$1");

        // Remove leftover brackets
        s = s.replace("[[", " ").replace("]]", " ");

        // Collapse whitespace
        s = s.replaceAll("\\s+", " ").trim();

        return s;
    }
}
