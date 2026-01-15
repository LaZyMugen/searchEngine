package com.shaswat.preprocess;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal tokenizer for v1:
 * - lowercase
 * - extracts [a-z0-9]+ tokens
 * - keeps token positions (0,1,2,...)
 */
public class Tokenizer {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[a-z0-9]+");

    public List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.isBlank()) return tokens;

        String lowered = text.toLowerCase();
        Matcher matcher = TOKEN_PATTERN.matcher(lowered);

        while (matcher.find()) {
            tokens.add(matcher.group());
        }

        return tokens;
    }
}
