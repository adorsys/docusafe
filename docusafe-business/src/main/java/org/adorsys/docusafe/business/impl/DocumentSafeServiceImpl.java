package org.adorsys.docusafe.business.impl;

import de.adorsys.dfs.connection.api.service.api.DFSConnection;
import de.adorsys.dfs.connection.api.types.ListRecursiveFlag;
import org.adorsys.docusafe.business.DocumentSafeService;
import org.adorsys.docusafe.business.types.MoveType;
import org.adorsys.docusafe.business.types.complex.DSDocument;
import org.adorsys.docusafe.business.types.complex.DSDocumentStream;
import org.adorsys.docusafe.business.types.complex.DocumentDirectoryFQN;
import org.adorsys.docusafe.business.types.complex.DocumentFQN;
import org.adorsys.docusafe.service.api.types.UserID;
import org.adorsys.docusafe.service.api.types.UserIDAuth;

import java.util.List;

public class DocumentSafeServiceImpl implements DocumentSafeService {
    public DocumentSafeServiceImpl(DFSConnection dfsConnection) {
    }

    @Override
    public void createUser(UserIDAuth userIDAuth) {

    }

    @Override
    public void destroyUser(UserIDAuth userIDAuth) {

    }

    @Override
    public boolean userExists(UserID userID) {
        return false;
    }

    @Override
    public void storeDocument(UserIDAuth userIDAuth, DSDocument dsDocument) {

    }

    @Override
    public DSDocument readDocument(UserIDAuth userIDAuth, DocumentFQN documentFQN) {
        return null;
    }

    @Override
    public void storeDocumentStream(UserIDAuth userIDAuth, DSDocumentStream dsDocumentStream) {

    }

    @Override
    public DSDocumentStream readDocumentStream(UserIDAuth userIDAuth, DocumentFQN documentFQN) {
        return null;
    }

    @Override
    public void deleteDocument(UserIDAuth userIDAuth, DocumentFQN documentFQN) {

    }

    @Override
    public boolean documentExists(UserIDAuth userIDAuth, DocumentFQN documentFQN) {
        return false;
    }

    @Override
    public void deleteFolder(UserIDAuth userIDAuth, DocumentDirectoryFQN documentDirectoryFQN) {

    }

    @Override
    public List<DocumentFQN> list(UserIDAuth userIDAuth, DocumentDirectoryFQN documentDirectoryFQN, ListRecursiveFlag recursiveFlag) {
        return null;
    }

    @Override
    public List<DocumentFQN> listInbox(UserIDAuth userIDAuth) {
        return null;
    }

    @Override
    public void writeDocumentToInboxOfUser(UserID receiverUserID, DSDocument document, DocumentFQN destDocumentFQN) {

    }

    @Override
    public DSDocument readDocumentFromInbox(UserIDAuth userIDAuth, DocumentFQN source) {
        return null;
    }

    @Override
    public void deleteDocumentFromInbox(UserIDAuth userIDAuth, DocumentFQN documentFQN) {

    }

    @Override
    public void moveDocumnetToInboxOfUser(UserIDAuth userIDAuth, UserID receiverUserID, DocumentFQN sourceDocumentFQN, DocumentFQN destDocumentFQN, MoveType moveType) {

    }

    @Override
    public DSDocument moveDocumentFromInbox(UserIDAuth userIDAuth, DocumentFQN source, DocumentFQN destination) {
        return null;
    }
}
