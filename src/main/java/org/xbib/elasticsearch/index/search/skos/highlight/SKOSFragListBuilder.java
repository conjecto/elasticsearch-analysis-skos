package org.xbib.elasticsearch.index.search.skos.highlight;

import org.apache.lucene.search.vectorhighlight.FieldFragList;
import org.apache.lucene.search.vectorhighlight.FieldPhraseList;
import org.apache.lucene.search.vectorhighlight.SimpleFragListBuilder;

/**
 * SKOSFragListBuilder
 */
public class SKOSFragListBuilder extends SimpleFragListBuilder {
    public SKOSFragListBuilder() {
    }

    public SKOSFragListBuilder(int margin) {
        super(margin);
    }

    public FieldFragList createFieldFragList(FieldPhraseList fieldPhraseList, int fragCharSize) {
        return this.createFieldFragList(fieldPhraseList, new SKOSFieldFragList(fragCharSize), fragCharSize);
    }
}
