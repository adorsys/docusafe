package org.adorsys.docusafe.business.impl;

import org.adorsys.docusafe.business.types.complex.BucketContentFQN;
import org.adorsys.docusafe.business.types.complex.DocumentDirectoryFQN;
import org.adorsys.docusafe.business.types.complex.DocumentFQN;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by peter on 14.06.18 at 11:33.
 */
public class BucketContentFQNImpl implements BucketContentFQN {
    private List<DocumentFQN> files = new ArrayList<>();

    @Override
    public List<DocumentFQN> getFiles() {
        return files;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BucketContentFQNImpl{");
        sb.append("\n");
        sb.append("files=");
        sb.append("\n");
        files.forEach(file -> sb.append("   " + file.getValue() + "\n"));
        sb.append("\n");
        sb.append("}");
        return sb.toString();
    }
}
