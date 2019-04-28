package de.adorsys.docusafe.business.types;

import java.util.List;

/**
 * Created by peter on 14.06.18 at 11:32.
 *
 * WICHTIG
 * Jeder DocumentFQN beginnt immer mit einem Slash, d.h. die Länge ist immer minimal 1
 */

public interface BucketContentFQN {
    List<DocumentFQN> getFiles();
}
