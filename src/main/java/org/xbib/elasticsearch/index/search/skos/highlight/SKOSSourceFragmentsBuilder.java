package org.xbib.elasticsearch.index.search.skos.highlight;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.vectorhighlight.BoundaryScanner;
import org.elasticsearch.index.mapper.FieldMapper;
import org.elasticsearch.search.fetch.FetchSubPhase;
import org.elasticsearch.search.internal.SearchContext;
import org.elasticsearch.search.lookup.SourceLookup;

import java.io.IOException;
import java.util.List;

/**
 * SourceSimpleFragmentsBuilder clone to extends SKOSFragmentsBuilder
 */
public class SKOSSourceFragmentsBuilder extends SKOSFragmentsBuilder {

    private final SearchContext searchContext;

    private final FetchSubPhase.HitContext hitContext;

    public SKOSSourceFragmentsBuilder(FieldMapper mapper, SearchContext searchContext,
                                        FetchSubPhase.HitContext hitContext, String[] preTags, String[] postTags, String htmlProperty, BoundaryScanner boundaryScanner) {
        super(mapper, preTags, postTags, htmlProperty, boundaryScanner);
        this.searchContext = searchContext;
        this.hitContext = hitContext;
    }

    public static final Field[] EMPTY_FIELDS = new Field[0];

    @Override
    protected Field[] getFields(IndexReader reader, int docId, String fieldName) throws IOException {
        // we know its low level reader, and matching docId, since that's how we call the highlighter with
        SourceLookup sourceLookup = searchContext.lookup().source();
        sourceLookup.setSegmentAndDocument((LeafReaderContext) reader.getContext(), docId);

        List<Object> values = sourceLookup.extractRawValues(hitContext.getSourcePath(mapper.fieldType().names().fullName()));
        if (values.isEmpty()) {
            return EMPTY_FIELDS;
        }
        Field[] fields = new Field[values.size()];
        for (int i = 0; i < values.size(); i++) {
            fields[i] = new Field(mapper.fieldType().names().indexName(), values.get(i).toString(), TextField.TYPE_NOT_STORED);
        }
        return fields;
    }
}
