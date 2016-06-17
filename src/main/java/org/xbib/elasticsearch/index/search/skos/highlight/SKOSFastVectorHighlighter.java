package org.xbib.elasticsearch.index.search.skos.highlight;

import com.google.common.collect.Maps;
import org.apache.lucene.search.highlight.DefaultEncoder;
import org.apache.lucene.search.highlight.Encoder;
import org.apache.lucene.search.highlight.SimpleHTMLEncoder;
import org.apache.lucene.search.vectorhighlight.*;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.mapper.FieldMapper;
import org.elasticsearch.search.fetch.FetchPhaseExecutionException;
import org.elasticsearch.search.fetch.FetchSubPhase;
import org.elasticsearch.search.highlight.*;
import org.elasticsearch.search.highlight.vectorhighlight.*;
import org.elasticsearch.search.internal.SearchContext;

import java.util.Collections;
import java.util.Map;

/**
 * Modified FastVectorHighlighter to handle skos tagging
 */
public class SKOSFastVectorHighlighter implements Highlighter {

    static Encoder DEFAULT = new DefaultEncoder();
    static Encoder HTML = new SimpleHTMLEncoder();

    private static final SimpleBoundaryScanner DEFAULT_BOUNDARY_SCANNER = new SimpleBoundaryScanner();

    private static final String CACHE_KEY = "skos-highlight-fsv";
    private final Boolean termVectorMultiValue;

    @Inject
    public SKOSFastVectorHighlighter(Settings settings) {
        this.termVectorMultiValue = settings.getAsBoolean("search.highlight.term_vector_multi_value", true);
    }

    @Override
    public HighlightField highlight(HighlighterContext highlighterContext) {
        SearchContextHighlight.Field field = highlighterContext.field;
        SearchContext context = highlighterContext.context;
        FetchSubPhase.HitContext hitContext = highlighterContext.hitContext;
        FieldMapper mapper = highlighterContext.mapper;

        if (canHighlight(mapper) == false) {
            throw new IllegalArgumentException("the field [" + highlighterContext.fieldName + "] should be indexed with term vector with position offsets to be used with fast vector highlighter");
        }

        Encoder encoder = field.fieldOptions().encoder().equals("html") ? this.HTML : this.DEFAULT;

        if (!hitContext.cache().containsKey(CACHE_KEY)) {
            hitContext.cache().put(CACHE_KEY, new HighlighterEntry());
        }
        HighlighterEntry cache = (HighlighterEntry) hitContext.cache().get(CACHE_KEY);

        try {
            FieldQuery fieldQuery;
            if (field.fieldOptions().requireFieldMatch()) {
                if (cache.fieldMatchFieldQuery == null) {
                    // we use top level reader to rewrite the query against all readers, with use caching it across hits (and across readers...)
                    cache.fieldMatchFieldQuery = new CustomFieldQuery(highlighterContext.query, hitContext.topLevelReader(), true, field.fieldOptions().requireFieldMatch());
                }
                fieldQuery = cache.fieldMatchFieldQuery;
            } else {
                if (cache.noFieldMatchFieldQuery == null) {
                    // we use top level reader to rewrite the query against all readers, with use caching it across hits (and across readers...)
                    cache.noFieldMatchFieldQuery = new CustomFieldQuery(highlighterContext.query, hitContext.topLevelReader(), true, field.fieldOptions().requireFieldMatch());
                }
                fieldQuery = cache.noFieldMatchFieldQuery;
            }

            MapperHighlightEntry entry = cache.mappers.get(mapper);
            if (entry == null) {
                FragListBuilder fragListBuilder;
                BaseFragmentsBuilder fragmentsBuilder;

                String htmlProperty = "uri";
                if(field.fieldOptions().options() != null && field.fieldOptions().options().containsKey("property")) {
                    htmlProperty = field.fieldOptions().options().get("property").toString();
                }

                BoundaryScanner boundaryScanner = DEFAULT_BOUNDARY_SCANNER;
                if (field.fieldOptions().boundaryMaxScan() != SimpleBoundaryScanner.DEFAULT_MAX_SCAN || field.fieldOptions().boundaryChars() != SimpleBoundaryScanner.DEFAULT_BOUNDARY_CHARS) {
                    boundaryScanner = new SimpleBoundaryScanner(field.fieldOptions().boundaryMaxScan(), field.fieldOptions().boundaryChars());
                }
                boolean forceSource = context.highlight().forceSource(field);
                if (field.fieldOptions().numberOfFragments() == 0) {
                    fragListBuilder = new SKOSFragListBuilder();

                    if (!forceSource && mapper.fieldType().stored()) {
                        // custom fragments builder
                        fragmentsBuilder = new SKOSFragmentsBuilder(mapper, field.fieldOptions().preTags(), field.fieldOptions().postTags(), htmlProperty, boundaryScanner);
                    } else {
                        fragmentsBuilder = new SourceSimpleFragmentsBuilder(mapper, context, hitContext, field.fieldOptions().preTags(), field.fieldOptions().postTags(), boundaryScanner);
                    }
                } else {
                    fragListBuilder = field.fieldOptions().fragmentOffset() == -1 ? new SKOSFragListBuilder() : new SKOSFragListBuilder(field.fieldOptions().fragmentOffset());
//                    if (field.fieldOptions().scoreOrdered()) {
//                        if (!forceSource && mapper.fieldType().stored()) {
//                            fragmentsBuilder = new ScoreOrderFragmentsBuilder(field.fieldOptions().preTags(), field.fieldOptions().postTags(), boundaryScanner);
//                        } else {
//                            fragmentsBuilder = new SourceScoreOrderFragmentsBuilder(mapper, context, hitContext, field.fieldOptions().preTags(), field.fieldOptions().postTags(), boundaryScanner);
//                        }
//                    } else {
                        if (!forceSource && mapper.fieldType().stored()) {
                            // custom fragments builder
                            fragmentsBuilder = new SKOSFragmentsBuilder(mapper, field.fieldOptions().preTags(), field.fieldOptions().postTags(), htmlProperty, boundaryScanner);
                        } else {
                            fragmentsBuilder = new SKOSSourceFragmentsBuilder(mapper, context, hitContext, field.fieldOptions().preTags(), field.fieldOptions().postTags(), htmlProperty, boundaryScanner);
                        }
//                    }
                }
                fragmentsBuilder.setDiscreteMultiValueHighlighting(termVectorMultiValue);
                entry = new MapperHighlightEntry();
                entry.fragListBuilder = fragListBuilder;
                entry.fragmentsBuilder = fragmentsBuilder;
                if (cache.fvh == null) {
                    // parameters to FVH are not requires since:
                    // first two booleans are not relevant since they are set on the CustomFieldQuery (phrase and fieldMatch)
                    // fragment builders are used explicitly
                    cache.fvh = new org.apache.lucene.search.vectorhighlight.FastVectorHighlighter();
                }
                CustomFieldQuery.highlightFilters.set(field.fieldOptions().highlightFilter());
                cache.mappers.put(mapper, entry);
            }
            cache.fvh.setPhraseLimit(field.fieldOptions().phraseLimit());

            String[] fragments;

            // a HACK to make highlighter do highlighting, even though its using the single frag list builder
            int numberOfFragments = field.fieldOptions().numberOfFragments() == 0 ? Integer.MAX_VALUE : field.fieldOptions().numberOfFragments();
            int fragmentCharSize = field.fieldOptions().numberOfFragments() == 0 ? Integer.MAX_VALUE : field.fieldOptions().fragmentCharSize();
            // we highlight against the low level reader and docId, because if we load source, we want to reuse it if possible
            // Only send matched fields if they were requested to save time.
            if (field.fieldOptions().matchedFields() != null && !field.fieldOptions().matchedFields().isEmpty()) {
                fragments = cache.fvh.getBestFragments(fieldQuery, hitContext.reader(), hitContext.docId(), mapper.fieldType().names().indexName(), field.fieldOptions().matchedFields(), fragmentCharSize,
                        numberOfFragments, entry.fragListBuilder, entry.fragmentsBuilder, field.fieldOptions().preTags(), field.fieldOptions().postTags(), encoder);
            } else {
                fragments = cache.fvh.getBestFragments(fieldQuery, hitContext.reader(), hitContext.docId(), mapper.fieldType().names().indexName(), fragmentCharSize,
                        numberOfFragments, entry.fragListBuilder, entry.fragmentsBuilder, field.fieldOptions().preTags(), field.fieldOptions().postTags(), encoder);
            }

            if (fragments != null && fragments.length > 0) {
                return new HighlightField(highlighterContext.fieldName, Text.convertFromStringArray(fragments));
            }

            int noMatchSize = highlighterContext.field.fieldOptions().noMatchSize();
            if (noMatchSize > 0) {
                // Essentially we just request that a fragment is built from 0 to noMatchSize using the normal fragmentsBuilder
                FieldFragList fieldFragList = new SimpleFieldFragList(-1 /*ignored*/);
                fieldFragList.add(0, noMatchSize, Collections.<FieldPhraseList.WeightedPhraseInfo>emptyList());
                fragments = entry.fragmentsBuilder.createFragments(hitContext.reader(), hitContext.docId(), mapper.fieldType().names().indexName(),
                        fieldFragList, 1, field.fieldOptions().preTags(), field.fieldOptions().postTags(), encoder);
                if (fragments != null && fragments.length > 0) {
                    return new HighlightField(highlighterContext.fieldName, Text.convertFromStringArray(fragments));
                }
            }

            return null;

        } catch (Exception e) {
            throw new FetchPhaseExecutionException(context, "Failed to highlight field [" + highlighterContext.fieldName + "]", e);
        }
    }

    @Override
    public boolean canHighlight(FieldMapper fieldMapper) {
        return fieldMapper.fieldType().storeTermVectors() && fieldMapper.fieldType().storeTermVectorOffsets() && fieldMapper.fieldType().storeTermVectorPositions();
    }

    private class MapperHighlightEntry {
        public FragListBuilder fragListBuilder;
        public FragmentsBuilder fragmentsBuilder;

        public org.apache.lucene.search.highlight.Highlighter highlighter;
    }

    private class HighlighterEntry {
        public org.apache.lucene.search.vectorhighlight.FastVectorHighlighter fvh;
        public FieldQuery noFieldMatchFieldQuery;
        public FieldQuery fieldMatchFieldQuery;
        public Map<FieldMapper, MapperHighlightEntry> mappers = Maps.newHashMap();
    }

}
