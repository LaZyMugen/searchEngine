package com.shaswat.index;

import java.util.ArrayList;
import java.util.List;

public class Posting {
    private final int docId;
    private final List<Integer> positions;

    public Posting(int docId) {
        this.docId = docId;
        this.positions = new ArrayList<>();
    }

    public int getDocId() {
        return docId;
    }

    public List<Integer> getPositions() {
        return positions;
    }

    public void addPosition(int pos) {
        positions.add(pos);
    }
}
