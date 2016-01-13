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
package org.xbib.elasticsearch.index.analysis.skos.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.xbib.elasticsearch.NodeTestUtils;
import org.xbib.elasticsearch.index.analysis.skos.engine.SKOSEngine;
import org.xbib.elasticsearch.index.analysis.skos.engine.SKOSEngineFactory;

/**
 * Tests the functionality of the Elasticsearch-backed SKOS Engine implementation
 */
public class SKOSEngineTest extends NodeTestUtils {

    @Test
    public void testSimpleSKOSSamplesRDFXML() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/skos_samples/simple_test_skos.rdf");
        SKOSEngine skosEngine = SKOSEngineFactory.getSKOSEngine(client("1"), "skos-1", inputStream, "RDF/XML");
        assertEquals(2, skosEngine.getAltTerms("quick").size());
        assertEquals(1, skosEngine.getAltTerms("over").size());
    }

    @Test
    public void testSimpleSKOSSamplesN3() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/skos_samples/simple_test_skos.rdf");
        SKOSEngine skosEngine = SKOSEngineFactory.getSKOSEngine(client("1"), "skos-2", inputStream, "RDF/XML");
        assertEquals(2, skosEngine.getAltTerms("quick").size());
        assertEquals(1, skosEngine.getAltTerms("over").size());
    }

    @Test
    public void testSimpleSKOSSampleN3NoType() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/skos_samples/simple_test_skos.n3");
        SKOSEngine skosEngine = SKOSEngineFactory.getSKOSEngine(client("1"), "skos-3", inputStream, "N3");
        assertEquals(2, skosEngine.getAltTerms("sheep").size());
        assertEquals(2, skosEngine.getAltTerms("kity").size());
    }

    @Test
    public void testSKOSSpecSamples() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/skos_samples/skos_spec_samples.n3");
        SKOSEngine skosEngine = SKOSEngineFactory.getSKOSEngine(client("1"), "skos-4", inputStream, "N3");
        assertEquals(skosEngine.getAltTerms("animals").size(), 3);
        assertEquals(skosEngine.getAltTerms("Food and Agriculture Organization").size(), 1);
    }

    @Test
    public void testSKOSSpecSamplesWithLanguageRestriction() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/skos_samples/skos_spec_samples.n3");
        SKOSEngine skosEngine = SKOSEngineFactory.getSKOSEngine(client("1"), "skos-5", inputStream, "N3", Collections.singletonList("en"));
        List<String> altTerms = skosEngine.getAltTerms("animals");
        assertEquals(1, altTerms.size());
        assertEquals("creatures", altTerms.get(0));
    }

    @Test
    public void testUKATSamples() throws IOException {
        InputStream skosFile = getClass().getResourceAsStream("/skos_samples/ukat_examples.n3");
        String conceptURI = "http://www.ukat.org.uk/thesaurus/concept/859";
        SKOSEngine skosEngine = SKOSEngineFactory.getSKOSEngine(client("1"), "skos-6", skosFile, "N3");
        // testing pref-labels
        List<String> prefLabel = skosEngine.getPrefLabels(conceptURI);
        assertEquals(1, prefLabel.size());
        assertEquals("weapons", prefLabel.get(0));
        // testing alt-labels
        List<String> altLabel = skosEngine.getAltLabels(conceptURI);
        assertEquals(2, altLabel.size());
        assertTrue(altLabel.contains("armaments"));
        assertTrue(altLabel.contains("arms"));
        // testing broader
        List<String> broader = skosEngine.getBroaderConcepts(conceptURI);
        assertEquals(1, broader.size());
        assertEquals("http://www.ukat.org.uk/thesaurus/concept/5060", broader.get(0));
        // testing narrower
        List<String> narrower = skosEngine.getNarrowerConcepts(conceptURI);
        assertEquals(2, narrower.size());
        assertTrue(narrower.contains("http://www.ukat.org.uk/thesaurus/concept/18874"));
        assertTrue(narrower.contains("http://www.ukat.org.uk/thesaurus/concept/7630"));
        // testing broader labels
        List<String> broaderLabels = skosEngine.getBroaderLabels(conceptURI);
        assertEquals(3, broaderLabels.size());
        assertTrue(broaderLabels.contains("military equipment"));
        assertTrue(broaderLabels.contains("defense equipment and supplies"));
        assertTrue(broaderLabels.contains("ordnance"));
        // testing narrower labels
        List<String> narrowerLabels = skosEngine.getNarrowerLabels(conceptURI);
        assertEquals(2, narrowerLabels.size());
        assertTrue(narrowerLabels.contains("ammunition"));
        assertTrue(narrowerLabels.contains("artillery"));
    }
}
