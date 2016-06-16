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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;

import org.apache.lucene.util.AttributeSource;
import org.xbib.elasticsearch.index.analysis.skos.engine.SKOSEngine;
import org.xbib.elasticsearch.index.analysis.skos.SKOSTypeAttribute.SKOSType;

/**
 * A Lucene TokenFilter that supports label-based term expansion as described in
 * https://code.google.com/p/lucene-skos/wiki/UseCases#UC2:_Label-based_term_expansion.
 *
 * It takes labels (String values) as input and searches a given SKOS vocabulary
 * for matching concepts (based on their prefLabels). If a match is found, it
 * adds the concept's labels to the output token stream.
 */
public final class SKOSLabelFilter extends AbstractSKOSFilter {

    public static final int DEFAULT_BUFFER_SIZE = 1;
    /* the size of the buffer used for multi-term prediction */
    private int bufferSize = DEFAULT_BUFFER_SIZE;
    /* a list serving as token buffer between consumed and consuming stream */
    private Queue<State> buffer = new LinkedList<>();

    /**
     * Constructor for multi-term expansion support. Takes an input token
     * stream, the SKOS engine, and an integer indicating the maximum token
     * length of the preferred labels in the SKOS vocabulary.
     *
     * @param input the consumed token stream
     * @param engine the skos expansion engine
     * @param analyzer the analyzer
     * @param bufferSize the length of the longest pref-label to consider
     * (needed for mult-term expansion)
     * @param types the skos types to expand to
     */
    public SKOSLabelFilter(TokenStream input, SKOSEngine engine,
            Analyzer analyzer, int bufferSize, List<SKOSType> types) {
        super(input, engine, analyzer, types);
        this.bufferSize = bufferSize;
    }

    /**
     * Advances the stream to the next token
     */
    @Override
    public boolean incrementToken() throws IOException {
        boolean next;
        while((next = input.incrementToken()) || !buffer.isEmpty()) {
            if(!next || buffer.size() == bufferSize) {
                addAliasesToStack();
                buffer.remove();
            }
            if(next) {
                buffer.add(input.captureState());
            }
        }
        if (termStack.size() > 0) {
            processTermOnStack();
            return true;
        }
        return false;
    }

    /**
     * @return
     * @throws IOException
     */
    private boolean addAliasesToStack() throws IOException {
        State entered = captureState();
        restoreState(buffer.peek());
        for (int i = buffer.size(); i > 0; i--) {
            BufferString inputTokens = bufferToString(i);
            addConceptsToStack(inputTokens);
//            if (addConceptsToStack(inputTokens)) {
//                break;
//            }
        }
        restoreState(entered);
        return !termStack.isEmpty();
    }

    /**
     * Converts the first x=noTokens states in the queue to a concatenated token
     * string separated by white spaces
     * @param noTokens the number of tokens
     * @return the concatenated token string
     */
    private BufferString bufferToString(int noTokens) {
        State entered = captureState();
        int endOffset = 0;
        State[] bufferedStates = buffer.toArray(new State[buffer.size()]);
        StringBuilder builder = new StringBuilder();
        builder.append(termAtt.toString());
        restoreState(bufferedStates[0]);
        for (int i = 1; i < noTokens; i++) {
            restoreState(bufferedStates[i]);
            endOffset = offsetAtt.endOffset();
            builder.append(" ").append(termAtt.toString());
        }
        restoreState(entered);
        return new BufferString(builder.toString(), entered, endOffset);
    }

    /**
     * Add terms to stack
     * Assumes that the given term is a textual token
     * @param term the given term
     * @return true if term stack is not empty
     */
    public boolean addConceptsToStack(BufferString term) throws IOException {
        List<String> conceptURIs = engine.getConcepts(term.getText());
        for (String conceptURI : conceptURIs) {

            pushLabelToStack(conceptURI, SKOSType.PREF, term.getState(), term.getEndOffset(), 1);
            pushLabelsToStack(engine.getBroaderConcepts(conceptURI), SKOSType.BROADER, term.getState(), term.getEndOffset());
            pushLabelsToStack(engine.getBroaderTransitiveConcepts(conceptURI), SKOSType.BROADERTRANSITIVE, term.getState(), term.getEndOffset());

            /*if (types.contains(SKOSType.PREF)) {
                pushLabelsToStack(engine.getPrefLabels(conceptURI), SKOSType.PREF);
            }
            if (types.contains(SKOSType.ALT)) {
                pushLabelsToStack(engine.getAltLabels(conceptURI), SKOSType.ALT);
            }
            if (types.contains(SKOSType.HIDDEN)) {
                pushLabelsToStack(engine.getHiddenLabels(conceptURI), SKOSType.HIDDEN);
            }
            if (types.contains(SKOSType.BROADER)) {
                pushLabelsToStack(engine.getBroaderLabels(conceptURI), SKOSType.BROADER);
            }
            if (types.contains(SKOSType.BROADERTRANSITIVE)) {
                pushLabelsToStack(engine.getBroaderTransitiveLabels(conceptURI), SKOSType.BROADERTRANSITIVE);
            }
            if (types.contains(SKOSType.NARROWER)) {
                pushLabelsToStack(engine.getNarrowerLabels(conceptURI), SKOSType.NARROWER);
            }
            if (types.contains(SKOSType.NARROWERTRANSITIVE)) {
                pushLabelsToStack(engine.getNarrowerTransitiveLabels(conceptURI), SKOSType.NARROWERTRANSITIVE);
            }*/
        }
        return !termStack.isEmpty();
    }

    /**
     * Helper class for capturing buffer string and states
     */
    protected static class BufferString {

        private final State state;
        private final int endOffset;
        private final String text;

        protected BufferString(String text, State state, int endOffset) {
            this.text = text;
            this.state = state;
            this.endOffset = endOffset;
        }

        protected String getText() {
            return this.text;
        }
        protected State getState() {
            return this.state;
        }
        protected int getEndOffset() {
            return this.endOffset;
        }
    }
}
