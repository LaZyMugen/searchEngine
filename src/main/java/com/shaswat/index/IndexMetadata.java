package com.shaswat.index;

public class IndexMetadata {
    public int N;
    public double avgTitleLen;
    public double avgBodyLen;
    public int[] titleLen;
    public int[] bodyLen;

    public IndexMetadata() {}

    public IndexMetadata(int N, double avgTitleLen, double avgBodyLen, int[] titleLen, int[] bodyLen) {
        this.N = N;
        this.avgTitleLen = avgTitleLen;
        this.avgBodyLen = avgBodyLen;
        this.titleLen = titleLen;
        this.bodyLen = bodyLen;
    }
}
