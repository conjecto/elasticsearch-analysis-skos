package org.xbib.elasticsearch.plugin.analysis;

import org.elasticsearch.index.analysis.AnalysisModule;
import org.elasticsearch.plugins.Plugin;

import org.elasticsearch.search.SearchModule;
import org.xbib.elasticsearch.index.search.skos.highlight.SKOSFastVectorHighlighter;
import org.xbib.elasticsearch.index.analysis.skos.SKOSAnalysisBinderProcessor;

public class SKOSAnalysisPlugin extends Plugin {

    @Override
    public String name() {
        return "analysis-skos";
    }

    @Override
    public String description() {
        return "SKOS analysis support";
    }

    public void onModule(AnalysisModule module) {
        module.addProcessor(new SKOSAnalysisBinderProcessor());
    }

    public void onModule(SearchModule highlightModule) {
        highlightModule.registerHighlighter("skos-fvh", SKOSFastVectorHighlighter.class);
    }

}

