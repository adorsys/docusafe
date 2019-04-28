package de.adorsys.docusafe.business.impl;

import de.adorsys.common.exceptions.BaseException;
import de.adorsys.dfs.connection.impl.factory.DFSConnectionFactory;
import de.adorsys.docusafe.business.DocumentSafeService;
import de.adorsys.docusafe.business.types.DSDocument;
import de.adorsys.docusafe.business.types.DocumentFQN;
import de.adorsys.docusafe.service.api.keystore.types.ReadKeyPassword;
import de.adorsys.docusafe.service.api.types.DocumentContent;
import de.adorsys.docusafe.service.api.types.UserID;
import de.adorsys.docusafe.service.api.types.UserIDAuth;

import java.util.Arrays;

/**
 * Created by peter on 19.02.19 09:45.
 */
public class ReadMeMDFileTestCode {
    public static void main(String[] args) {
        // create service
        DocumentSafeService documentSafeService = new DocumentSafeServiceImpl(DFSConnectionFactory.get());

        // create user
        UserIDAuth userIDAuth = new UserIDAuth(new UserID("peter"), new ReadKeyPassword("passwordOfPeter"));
        documentSafeService.createUser(userIDAuth);

        // create document
        DocumentFQN documentFQN = new DocumentFQN("first/document.txt");
        DocumentContent documentContent = new DocumentContent(("programming is the mirror of your mind").getBytes());
        DSDocument dsDocument = new DSDocument(documentFQN, documentContent);
        documentSafeService.storeDocument(userIDAuth, dsDocument);

        // read the document again
        DSDocument dsDocumentRead = documentSafeService.readDocument(userIDAuth, documentFQN);
        if (Arrays.equals(dsDocument.getDocumentContent().getValue(), dsDocumentRead.getDocumentContent().getValue()) == true) {
            System.out.println("read the following content from " + documentFQN + ":" + new String(dsDocumentRead.getDocumentContent().getValue()));
        } else {
            throw new BaseException("This will never happen :-)");
        }
    }
}
