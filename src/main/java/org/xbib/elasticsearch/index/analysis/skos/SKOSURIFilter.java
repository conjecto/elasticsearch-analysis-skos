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
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;

import org.xbib.elasticsearch.index.analysis.skos.engine.SKOSEngine;
import org.xbib.elasticsearch.index.analysis.skos.SKOSTypeAttribute.SKOSType;

/**
 * A Lucene TokenFilter that supports URI-based term expansion as described in
 * https://code.
 * google.com/p/lucene-skos/wiki/UseCases#UC1:_URI-based_term_expansion
 *
 * It takes references to SKOS concepts (URIs) as input and searches a given
 * SKOS vocabulary for matching concepts. If a match is found, it adds the
 * concept's labels to the output token stream.
 */
public final class SKOSURIFilter extends AbstractSKOSFilter {

    public SKOSURIFilter(TokenStream input, SKOSEngine skosEngine,
            Analyzer analyzer, List<SKOSType> types) {
        super(input, skosEngine, analyzer, types);
    }

    /**
     * Advances the stream to the next token
     */
    @Override
    public boolean incrementToken() throws IOException {
        /* there are expanded terms for the given token */
        if (termStack.size() > 0) {
            processTermOnStack();
            return true;
        }
        /* no more tokens on the consumed stream -> end of stream */
        if (!input.incrementToken()) {
            return false;
        }
        /* check whether there are expanded terms for a given token */
        addTermsToStack(termAtt.toString());
        return true;
    }

    /**
     * Assumes that the given term is a concept URI
     * @param term the given term
     * @return true if term stack is not empty
     */
    public boolean addTermsToStack(String term) throws IOException {
        State state = captureState();

        if (types.contains(SKOSType.PREF)) {
            pushLabelsToStack(engine.getPrefLabels(term), SKOSType.PREF, state, 0);
        }
        if (types.contains(SKOSType.ALT)) {
            pushLabelsToStack(engine.getAltLabels(term), SKOSType.ALT, state, 0);
        }
        if (types.contains(SKOSType.BROADER)) {
            pushLabelsToStack(engine.getBroaderLabels(term), SKOSType.BROADER, state, 0);
        }
        if (types.contains(SKOSType.BROADERTRANSITIVE)) {
            pushLabelsToStack(engine.getBroaderTransitiveLabels(term), SKOSType.BROADERTRANSITIVE, state, 0);
        }
        if (types.contains(SKOSType.NARROWER)) {
            pushLabelsToStack(engine.getNarrowerLabels(term), SKOSType.NARROWER, state, 0);
        }
        if (types.contains(SKOSType.NARROWERTRANSITIVE)) {
            pushLabelsToStack(engine.getNarrowerTransitiveLabels(term), SKOSType.NARROWERTRANSITIVE, state, 0);
        }
        return !termStack.isEmpty();
    }
}
