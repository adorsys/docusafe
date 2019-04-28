package de.adorsys.docusafe.transactional.impl.helper;

import de.adorsys.dfs.connection.api.complextypes.BucketPath;
import de.adorsys.docusafe.business.types.DocumentDirectoryFQN;
import de.adorsys.docusafe.business.types.DocumentFQN;
import de.adorsys.docusafe.transactional.types.TxBucketContentFQN;
import de.adorsys.docusafe.transactional.types.TxDocumentFQNVersion;
import de.adorsys.docusafe.transactional.types.TxDocumentFQNWithVersion;
import de.adorsys.docusafe.transactional.types.TxID;
import de.adorsys.docusafe.transactional.impl.TxBucketContentFQNImpl;
import de.adorsys.dfs.connection.api.types.ListRecursiveFlag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Created by peter on 14.06.18 at 17:54.
 */
public class BucketContentFromHashMapHelper {
    public static TxBucketContentFQN list(Map<DocumentFQN, TxID> documentTxMap, DocumentDirectoryFQN documentDirectoryFQN, ListRecursiveFlag recursiveFlag) {
        List<TxDocumentFQNWithVersion> candidates = new ArrayList<>();
        documentTxMap.keySet().forEach(documentFQN -> {
            if (documentFQN.getValue().startsWith(documentDirectoryFQN.getValue())) {
                candidates.add(new TxDocumentFQNWithVersion(documentFQN, new TxDocumentFQNVersion(documentTxMap.get(documentFQN).getValue())));
            }
        });

        // finden aller Verzeichnisse
        // search:    /a/
        // candidate: /a/b/c/file1
        // result:    /a/b/
        //            /a/b/c

        candidates.forEach(candidate -> {
            DocumentFQN remainder = new DocumentFQN(candidate.getDocumentFQN().getValue().substring(documentDirectoryFQN.getValue().length()));
            // candidate /a/b/c/file1
            // search    /a
            // remainder   /b/c/file1

            String dirPath = remainder.getDocumentDirectory().getValue();
            // dirpath     /b/c
            StringTokenizer st = new StringTokenizer(dirPath, BucketPath.BUCKET_SEPARATOR);
            String dirbase = documentDirectoryFQN.getValue();
            if (dirbase.length() == 1) {
                dirbase = "";
            }
            while (st.hasMoreElements()) {
                dirbase = dirbase + BucketPath.BUCKET_SEPARATOR + st.nextToken();
            }
        });

        if (recursiveFlag.equals(ListRecursiveFlag.TRUE)) {
            TxBucketContentFQN bucketContentFQN = new TxBucketContentFQNImpl();
            candidates.forEach(candidate -> bucketContentFQN.getFiles().add(candidate.getDocumentFQN()));
            candidates.forEach(candidate -> bucketContentFQN.getFilesWithVersion().add(candidate));
            return bucketContentFQN;
        }

        // reduzieren
        //
        TxBucketContentFQN bucketContentFQN = new TxBucketContentFQNImpl();
        candidates.forEach(candidate -> {
            DocumentFQN remainder = new DocumentFQN(candidate.getDocumentFQN().getValue().substring(documentDirectoryFQN.getValue().length()));
            // candidate /a/b/c/file1
            // search    /a
            // remainder   /b/c/file1
            if (remainder.getValue().lastIndexOf(BucketPath.BUCKET_SEPARATOR) == 0) {
                bucketContentFQN.getFiles().add(candidate.getDocumentFQN());
                bucketContentFQN.getFilesWithVersion().add(candidate);
            }
        });

        return bucketContentFQN;
    }
}
