package de.adorsys.docusafe.cached.transactional;

import de.adorsys.docusafe.transactional.TransactionalDocumentSafeService;

/**
 * Created by peter on 21.06.18 at 11:49.
 *
 * Das Caching bezieht sich ausschließlich auf Aktionen, die innerhalb einer Transaktion stattfinden.
 * Die Nicht-transaktionalen Aktionen werden nicht gecached.
 */
public interface CachedTransactionalDocumentSafeService extends TransactionalDocumentSafeService {
}


