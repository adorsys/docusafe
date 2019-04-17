package org.adorsys.docusafe.transactional.types;

import org.adorsys.docusafe.business.types.BucketContentFQN;

import java.util.List;

/**
 * Created by peter on 30.01.19 09:58.
 */
public interface TxBucketContentFQN extends BucketContentFQN {
    List<TxDocumentFQNWithVersion> getFilesWithVersion();
}
