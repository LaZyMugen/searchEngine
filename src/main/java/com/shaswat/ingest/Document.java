package com.shaswat.ingest;

public class Document {
    private int docId;
    private String title;
    private String body;

    // Jackson needs a no-arg constructor
    public Document() {}

    public Document(int docId, String title, String body) {
        this.docId = docId;
        this.title = title;
        this.body = body;
    }

    public int getDocId() {
        return docId;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }
}
