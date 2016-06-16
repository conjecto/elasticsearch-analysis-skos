package org.xbib.elasticsearch.index.analysis.skos;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.analysis.AbstractTokenFilterFactory;
import org.elasticsearch.index.analysis.AnalysisSettingsRequired;
import org.elasticsearch.index.settings.IndexSettingsService;
import org.xbib.elasticsearch.index.analysis.skos.SKOSAnalyzer.ExpansionType;
import org.xbib.elasticsearch.index.analysis.skos.SKOSLabelFilter;
import org.xbib.elasticsearch.index.analysis.skos.SKOSTypeAttribute.SKOSType;
import org.xbib.elasticsearch.index.analysis.skos.SKOSURIFilter;
import org.xbib.elasticsearch.index.analysis.skos.engine.SKOSEngine;
import org.xbib.elasticsearch.index.analysis.skos.engine.SKOSEngineFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@AnalysisSettingsRequired
public class ConcatenateFilterFactory extends AbstractTokenFilterFactory {

    private String tokenSeparator = null;

    @Inject
    public ConcatenateFilterFactory(Index index,
                                    IndexSettingsService indexSettingsService,
                                    @Assisted String name,
                                    @Assisted Settings settings,
                                    Injector injector) {
        super(index,  indexSettingsService.indexSettings(), name, settings);
        // the token_separator is defined in the ES configuration file
        tokenSeparator = settings.get("token_separator");
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return new ConcatenateFilter(tokenStream, tokenSeparator);
    }
}
