package org.xbib.elasticsearch.index.search.skos.highlight;

import org.apache.lucene.search.vectorhighlight.FieldPhraseList;
import org.apache.lucene.search.vectorhighlight.FieldTermStack;
import org.apache.lucene.search.vectorhighlight.SimpleFieldFragList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * SKOSFieldFragList
 */
public class SKOSFieldFragList extends SimpleFieldFragList {
    public SKOSFieldFragList(int fragCharSize) {
        super(fragCharSize);
    }

    public void add(int startOffset, int endOffset, List<FieldPhraseList.WeightedPhraseInfo> phraseInfoList) {
        float totalBoost = 0.0F;
        ArrayList subInfos = new ArrayList();

        FieldPhraseList.WeightedPhraseInfo phraseInfo;
        for(Iterator i$ = phraseInfoList.iterator(); i$.hasNext(); totalBoost += phraseInfo.getBoost()) {
            phraseInfo = (FieldPhraseList.WeightedPhraseInfo)i$.next();
            StringBuilder text = new StringBuilder();
            Iterator j$ = phraseInfo.getTermsInfos().iterator();
            while(j$.hasNext()) {
                if(text.length() > 0) {
                    text.append("|");
                }
                FieldTermStack.TermInfo ti = (FieldTermStack.TermInfo)j$.next();
                text.append(ti.getText());
            }
            subInfos.add(new WeightedFragInfo.SubInfo(text.toString(), phraseInfo.getTermsOffsets(), phraseInfo.getSeqnum(), phraseInfo.getBoost()));
        }

        this.getFragInfos().add(new WeightedFragInfo(startOffset, endOffset, subInfos, totalBoost));
    }
}
