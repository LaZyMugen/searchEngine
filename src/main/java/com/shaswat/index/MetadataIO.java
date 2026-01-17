package com.shaswat.index;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

public class MetadataIO {
    private final ObjectMapper mapper = new ObjectMapper();

    public void write(String path, IndexMetadata meta) throws Exception {
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(path), meta);
    }

    public IndexMetadata read(String path) throws Exception {
        return mapper.readValue(new File(path), IndexMetadata.class);
    }
}
