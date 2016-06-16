/**
 * Copyright 2010 Bernhard Haslhofer
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.xbib.elasticsearch.index.analysis.skos;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.payloads.PayloadHelper;
import org.apache.lucene.analysis.tokenattributes.*;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.CharsRef;

import org.apache.lucene.util.CharsRefBuilder;
import org.xbib.elasticsearch.index.analysis.skos.engine.SKOSEngine;
import org.xbib.elasticsearch.index.analysis.skos.SKOSTypeAttribute.SKOSType;

/**
 * Basic methods for SKOS-specific TokenFilter implementations
 */
public abstract class AbstractSKOSFilter extends TokenFilter {

    // a stack holding the expanded terms for a token
    protected Stack<ExpandedTerm> termStack;
    // an engine delivering SKOS concepts
    protected SKOSEngine engine;
    // the skos types to expand to
    protected Set<SKOSType> types;
    // the term text (propagated to the index)
    protected final CharTermAttribute termAtt;
    // the token position relative to the previous token (propagated)
    protected final PositionIncrementAttribute posIncrAtt;
    // the token position relative to the previous token (propagated)
    protected final OffsetAttribute offsetAtt;
    // the binary payload attached to the indexed term (propagated to the index)
    protected final PayloadAttribute payloadAtt;
    // the SKOS-specific attribute attached to a term
    protected final SKOSTypeAttribute skosAtt;
    // the analyzer to use when parsing
    protected final Analyzer analyzer;

    private List<SKOSTypeAttribute.SKOSType> defaultTypes = Arrays.asList(SKOSAnalyzer.DEFAULT_SKOS_TYPES);

    /**
     * Constructor
     *
     * @param input the TokenStream
     * @param engine the engine delivering skos concepts
     * @param analyzer the analyzer
     * @param types the skos types to expand to
     */
    public AbstractSKOSFilter(TokenStream input, SKOSEngine engine, Analyzer analyzer, List<SKOSType> types) {
        super(input);
        termStack = new Stack<>();
        this.engine = engine;
        this.analyzer = analyzer;
        this.types = new TreeSet<>(types != null && !types.isEmpty() ? types : defaultTypes);
        this.termAtt = addAttribute(CharTermAttribute.class);
        this.posIncrAtt = addAttribute(PositionIncrementAttribute.class);
        this.payloadAtt = addAttribute(PayloadAttribute.class);
        this.skosAtt = addAttribute(SKOSTypeAttribute.class);
        this.offsetAtt = addAttribute(OffsetAttribute.class);
    }

    /**
     * Advances the stream to the next token.
     *
     * To be implemented by the concrete sub-classes
     */
    @Override
    public abstract boolean incrementToken() throws IOException;

    /**
     * Replaces the current term (attributes) with term (attributes) from the stack
     *
     * @throws IOException if analyzer failed
     */
    protected void processTermOnStack() throws IOException {
        // dont use termStack.pop() to handle correct order
        ExpandedTerm expandedTerm = termStack.firstElement();
        termStack.remove(0);
        String term = expandedTerm.getTerm();
        SKOSType termType = expandedTerm.getTermType();
        String sTerm = term;
        // copies the values of all attribute implementations from this state into
        // the implementations of the target stream
        restoreState(expandedTerm.getState());
        // adds the expanded term to the term buffer
        termAtt.setEmpty().append(sTerm);
        // change endoffset in needed
        if(expandedTerm.getEndOffset() > 0) {
            offsetAtt.setOffset(offsetAtt.startOffset(), expandedTerm.getEndOffset());
        }
        // set position increment to zero to put multiple terms into the same position
        posIncrAtt.setPositionIncrement(expandedTerm.getPosIncr());
        // sets the type of the expanded term (pref, alt, broader, narrower, etc.)
        skosAtt.setSkosType(termType);
        // converts the SKOS Attribute to a payload, which is propagated to the index
        byte[] bytes = PayloadHelper.encodeInt(skosAtt.getSkosType().ordinal());
        payloadAtt.setPayload(new BytesRef(bytes));
    }

    protected void pushLabelsToStack(List<String> labels, SKOSType type, State state, int endOffset) {
        if (labels != null) {
            for (String label : labels) {
                pushLabelToStack(label, type, state, endOffset, 0);
            }
        }
    }

    protected void pushLabelToStack(String label, SKOSType type, State state, int endOffset, int posIncr) {
        termStack.push(new ExpandedTerm(label, type, state, endOffset, posIncr));
    }

    /**
     * Helper class for capturing terms and term types
     */
    protected static class ExpandedTerm {

        private final String term;
        private final SKOSType termType;
        private final State state;
        private final int endOffset;
        private final int posIncr;

        protected ExpandedTerm(String term, SKOSType termType, State state, int endOffset, int posIncr) {
            this.term = term;
            this.termType = termType;
            this.state = state;
            this.endOffset = endOffset;
            this.posIncr = posIncr;
        }

        protected String getTerm() { return this.term; }
        protected SKOSType getTermType() { return this.termType; }
        protected State getState() { return this.state; }
        protected int getEndOffset() { return this.endOffset; }
        protected int getPosIncr() { return this.posIncr; }
    }
}
