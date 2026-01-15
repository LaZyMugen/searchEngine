package com.shaswat.ingest;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;

public class WikiDumpParser {

    private final ObjectMapper mapper = new ObjectMapper();

    public int parseToJsonl(String inputXmlPath, String outputJsonlPath, int maxDocs) throws Exception {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        // Security best practice: disable external entities
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);

        int docId = 0;

        try (FileInputStream fis = new FileInputStream(inputXmlPath);
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputJsonlPath, StandardCharsets.UTF_8))) {

            XMLStreamReader reader = factory.createXMLStreamReader(fis);

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
                    // Fix: Do not skip whitespace if it's significant (inside title or text)
                    if (reader.isWhiteSpace()) {
                        if (!"title".equals(currentTag) && !"text".equals(currentTag)) {
                             continue;
                        }
                    }

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
                        
                        String title = (titleBuffer != null) ? titleBuffer.toString() : null;
                        String text = (textBuffer != null) ? textBuffer.toString() : null;
                        
                       boolean hasContent = title != null && !title.isBlank() && text != null && !text.isBlank();


                        if (isMainNamespace && !isRedirect && hasContent) {
                            // minimal cleaning for v1 (more later)
                            String cleanedBody = text.replaceAll("\\s+", " ").trim();

                            Document doc = new Document(docId, title.trim(), cleanedBody);

                            writer.write(mapper.writeValueAsString(doc));
                            writer.newLine();

                            docId++;
                            if (docId % 500 == 0) System.out.println("Processed " + docId + " documents");

                            if (docId >= maxDocs) break;
                        }
                    }
                }
            }

            reader.close();
        }

        return docId;
    }
}
