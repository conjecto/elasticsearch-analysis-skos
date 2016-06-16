
package org.xbib.elasticsearch.index.analysis.skos;

import org.elasticsearch.index.analysis.AnalysisModule;

public class SKOSAnalysisBinderProcessor extends AnalysisModule.AnalysisBinderProcessor {

    @Override
    public void processTokenFilters(TokenFiltersBindings tokenFilterBindings) {
        tokenFilterBindings.processTokenFilter("skos", SKOSTokenFilterFactory.class);
        tokenFilterBindings.processTokenFilter("concatenate", ConcatenateFilterFactory.class);
    }

}


