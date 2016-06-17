package org.xbib.elasticsearch.index.search.skos.highlight;

import org.apache.lucene.document.Field;
import org.apache.lucene.search.highlight.Encoder;
import org.apache.lucene.search.vectorhighlight.BoundaryScanner;
import org.apache.lucene.search.vectorhighlight.FieldFragList;
import org.apache.lucene.search.vectorhighlight.FieldPhraseList;
import org.elasticsearch.index.mapper.FieldMapper;
import org.elasticsearch.search.highlight.vectorhighlight.SimpleFragmentsBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * SKOSFragmentsBuilder
 */
public class SKOSFragmentsBuilder extends SimpleFragmentsBuilder {
    protected String htmlProperty;

    public SKOSFragmentsBuilder(FieldMapper mapper,
                                String[] preTags, String[] postTags, String htmlProperty, BoundaryScanner boundaryScanner) {
        super(mapper, preTags, postTags, boundaryScanner);
        this.htmlProperty = htmlProperty;
    }

    protected String makeFragment(StringBuilder buffer, int[] index, Field[] values, FieldFragList.WeightedFragInfo fragInfo, String[] preTags, String[] postTags, Encoder encoder) {
        StringBuilder fragment = new StringBuilder();
        int s = fragInfo.getStartOffset();
        int[] modifiedStartOffset = new int[]{s};
        String src = this.getFragmentSourceMSO(buffer, index, values, s, fragInfo.getEndOffset(), modifiedStartOffset);
        int srcIndex = 0;
        Iterator i$ = fragInfo.getSubInfos().iterator();

        while(i$.hasNext()) {
            FieldFragList.WeightedFragInfo.SubInfo subInfo = (FieldFragList.WeightedFragInfo.SubInfo)i$.next();

            FieldPhraseList.WeightedPhraseInfo.Toffs to;
            for(Iterator i$1 = subInfo.getTermsOffsets().iterator(); i$1.hasNext(); srcIndex = to.getEndOffset() - modifiedStartOffset[0]) {
                to = (FieldPhraseList.WeightedPhraseInfo.Toffs)i$1.next();
                fragment.append(encoder.encodeText(src.substring(srcIndex, to.getStartOffset() - modifiedStartOffset[0])))
                        .append(this.getPreTag(preTags, subInfo))
                        .append(encoder.encodeText(src.substring(to.getStartOffset() - modifiedStartOffset[0], to.getEndOffset() - modifiedStartOffset[0])))
                        .append(this.getPostTag(postTags, subInfo.getSeqnum()));
            }
        }

        fragment.append(encoder.encodeText(src.substring(srcIndex)));
        return fragment.toString();
    }

    protected String getPreTag(String[] preTags, FieldFragList.WeightedFragInfo.SubInfo subInfo) {
        int n = subInfo.getSeqnum() % preTags.length;
        String preTag = preTags[n];
        List<String> uris = Arrays.asList(subInfo.getText().split("\\|"));
        for (String uri : uris) {
            preTag = preTag.replaceAll(">", " "+ this.htmlProperty +"=\"" + uri + "\">");
        }
        return preTag;
    }
}
