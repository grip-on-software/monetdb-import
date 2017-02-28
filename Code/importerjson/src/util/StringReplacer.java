/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;
import org.ahocorasick.trie.Trie.TrieBuilder;

/**
 * Class that perofrms fast string replacements.
 * @author Leon Helwerda
 */
public class StringReplacer {
    Map<String, String> definitions;
    TrieBuilder builder;
    Trie trie;
    boolean elongating;
    
    public StringReplacer() {
        definitions = new HashMap<>();
        builder = Trie.builder();
        trie = null;
        elongating = false;
    }
    
    public StringReplacer add(String search, String replace) {
        builder.addKeyword(search);
        definitions.put(search, replace);
        if (search.length() < replace.length()) {
            elongating = true;
        }
        // Invalidate the trie
        trie = null;
        return this;
    }
    
    public String execute(String text) {
        // Based on code from http://stackoverflow.com/a/40836618
        // Create a buffer sufficiently large that re-allocations are minimized.
        int length = text.length();
        final StringBuilder sb = new StringBuilder(elongating ? length << 1 : length);
        
        if (trie == null) {
            trie = builder.build();
        }

        final Collection<Emit> emits = trie.parseText(text);

        int prevIndex = 0;

        for (final Emit emit : emits) {
            final int matchIndex = emit.getStart();

            sb.append(text, prevIndex, matchIndex);
            sb.append(definitions.get(emit.getKeyword()));
            prevIndex = emit.getEnd() + 1;
        }

        // Add the remainder of the string (contains no more matches).
        sb.append(text, prevIndex, length);

        return sb.toString();        
    }
}
