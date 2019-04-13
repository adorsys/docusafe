package org.adorsys.docusafe.transactional.impl.helper;

import de.adorsys.dfs.connection.api.complextypes.BucketPath;
import org.adorsys.docusafe.business.types.complex.DocumentDirectoryFQN;
import org.adorsys.docusafe.business.types.complex.DocumentFQN;
import org.adorsys.docusafe.transactional.types.TxBucketContentFQN;
import org.adorsys.docusafe.transactional.types.TxDocumentFQNVersion;
import org.adorsys.docusafe.transactional.types.TxDocumentFQNWithVersion;
import org.adorsys.docusafe.transactional.impl.TxBucketContentFQNImpl;
import org.adorsys.docusafe.transactional.types.TxID;
import de.adorsys.dfs.connection.api.types.ListRecursiveFlag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

        Set<DocumentDirectoryFQN> dirCandidates = new HashSet<>();

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
                dirCandidates.add(new DocumentDirectoryFQN(dirbase));
                // fügt erst /a/b
                // dann      /a/b/c
                // ein
            }
        });

        if (recursiveFlag.equals(ListRecursiveFlag.TRUE)) {
            TxBucketContentFQN bucketContentFQN = new TxBucketContentFQNImpl();
            candidates.forEach(candidate -> bucketContentFQN.getFiles().add(candidate.getDocumentFQN()));
            dirCandidates.forEach(dirCandidate -> bucketContentFQN.getDirectories().add(dirCandidate));
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

        HashSet<DocumentDirectoryFQN> dirs = new HashSet<>();
        dirCandidates.forEach(dirCandidate -> {
            DocumentDirectoryFQN remainder = new DocumentDirectoryFQN(dirCandidate.getValue().substring(documentDirectoryFQN.getValue().length()));
            // dirCandidate /a/b/c
            // search       /a
            // remainder      /b/c
            if (remainder.getValue().lastIndexOf(BucketPath.BUCKET_SEPARATOR) == 0) {
                dirs.add(remainder);
            }
        });
        dirs.forEach(dir -> bucketContentFQN.getDirectories().add(dir));
        return bucketContentFQN;
    }
}
