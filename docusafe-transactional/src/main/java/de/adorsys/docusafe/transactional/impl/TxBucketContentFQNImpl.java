package de.adorsys.docusafe.transactional.impl;

import de.adorsys.docusafe.business.types.BucketContentFQNImpl;
import de.adorsys.docusafe.transactional.types.TxBucketContentFQN;
import de.adorsys.docusafe.transactional.types.TxDocumentFQNWithVersion;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by peter on 30.01.19 10:20.
 */
public class TxBucketContentFQNImpl extends BucketContentFQNImpl implements TxBucketContentFQN {
    private List<TxDocumentFQNWithVersion> txDocumentFQNWithVersionList = new ArrayList<>();

    @Override
    public List<TxDocumentFQNWithVersion> getFilesWithVersion() {
        return txDocumentFQNWithVersionList;
    }
}
