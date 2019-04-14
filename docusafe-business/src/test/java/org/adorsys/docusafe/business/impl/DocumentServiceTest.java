package org.adorsys.docusafe.business.impl;

import de.adorsys.dfs.connection.impl.factory.DFSConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.adorsys.docusafe.business.DocumentSafeService;
import org.adorsys.docusafe.service.api.keystore.types.ReadKeyPassword;
import org.adorsys.docusafe.service.api.types.UserID;
import org.adorsys.docusafe.service.api.types.UserIDAuth;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public class DocumentServiceTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(DocumentServiceTest.class);

    DocumentSafeService service = new DocumentSafeServiceImpl(DFSConnectionFactory.get());
    UserIDAuth userIDAuth = new UserIDAuth(new UserID("peter"), new ReadKeyPassword("affe"));


    @Test
    public void a() {
        service.createUser(userIDAuth);
        LOGGER.debug("OK ");
        service.destroyUser(userIDAuth);

    }
}
