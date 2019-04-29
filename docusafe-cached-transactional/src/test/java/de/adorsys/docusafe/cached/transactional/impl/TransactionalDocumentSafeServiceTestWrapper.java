package de.adorsys.docusafe.cached.transactional.impl;

import de.adorsys.docusafe.business.types.*;
import de.adorsys.docusafe.service.api.types.UserID;
import de.adorsys.docusafe.service.api.types.UserIDAuth;
import de.adorsys.docusafe.transactional.TransactionalDocumentSafeService;
import de.adorsys.docusafe.transactional.types.TxBucketContentFQN;
import de.adorsys.docusafe.transactional.types.TxDocumentFQNVersion;
import de.adorsys.dfs.connection.api.types.ListRecursiveFlag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by peter on 09.07.18 at 14:40.
 */
public class TransactionalDocumentSafeServiceTestWrapper implements TransactionalDocumentSafeService {
    int MAX = 30;

    public static final String CREATE_USER = "createUser";
    public static final String DESTROY_USER = "destroyUser";
    public static final String USER_EXISTS = "userExists";
    public static final String BEGIN_TX = "beginTx";
    public static final String TX_STORE_DOCUMENT = "storeDocumentTx";
    public static final String TX_READ_DOCUMENT = "readDocumentTx";
    public static final String TX_DELETE_DOCUMENT = "deleteDocumentTx";
    public static final String TX_LIST_DOCUMENTS = "listDocumentsTx";
    public static final String TX_DOCUMENT_EXISTS = "documentExistsTx";
    public static final String TX_DELETE_FOLDER = "deleteFolderTx";
    public static final String END_TRANSACTION = "endTransaction";
    public static final String FIND_PUBLIC_KEY = "findPublicEncryptionKey";
    public static final String GET_VERSION = "txGetVersion";
    public static final String NON_TX_LIST_INBOX = "nonTxListInbox";
    public static final String TX_MOVE_DOCUMENT_TO_INBOX_OF_USER = "txMoveDocumentToInboxOfUser";
    public static final String TX_MOVE_FROM_INBOX = "txmoveDocumentFromInbox";

    public Map<String, Integer> counterMap = new HashMap<>();

    private TransactionalDocumentSafeService realTransactionalFileStorage;
    public TransactionalDocumentSafeServiceTestWrapper(TransactionalDocumentSafeService realTransactionalFileStorage) {
        this.realTransactionalFileStorage = realTransactionalFileStorage;
        inc(CREATE_USER);
        inc(DESTROY_USER);
        inc(USER_EXISTS);
        inc(BEGIN_TX);
        inc(TX_STORE_DOCUMENT);
        inc(TX_READ_DOCUMENT);
        inc(TX_DELETE_DOCUMENT);
        inc(TX_LIST_DOCUMENTS);
        inc(TX_DOCUMENT_EXISTS);
        inc(TX_DELETE_FOLDER);
        inc(END_TRANSACTION);
        inc(FIND_PUBLIC_KEY);
        inc(GET_VERSION);
        inc(NON_TX_LIST_INBOX);
        inc(TX_MOVE_DOCUMENT_TO_INBOX_OF_USER);
        inc(TX_MOVE_FROM_INBOX);
    }

    @Override
    public void createUser(UserIDAuth userIDAuth) {
        inc(CREATE_USER);
        realTransactionalFileStorage.createUser(userIDAuth);
    }

    @Override
    public void destroyUser(UserIDAuth userIDAuth) {
        inc(DESTROY_USER);
        realTransactionalFileStorage.destroyUser(userIDAuth);
    }

    @Override
    public boolean userExists(UserID userID) {
        inc(USER_EXISTS);
        return realTransactionalFileStorage.userExists(userID);
    }

    @Override
    public void registerDFSCredentials(UserIDAuth userIDAuth, DFSCredentials dfsCredentials) {
        realTransactionalFileStorage.registerDFSCredentials(userIDAuth, dfsCredentials);
    }

    @Override
    public List<DocumentFQN> nonTxListInbox(UserIDAuth userIDAuth) {
        inc(NON_TX_LIST_INBOX);
        return realTransactionalFileStorage.nonTxListInbox(userIDAuth);
    }

    @Override
    public void beginTransaction(UserIDAuth userIDAuth) {
        inc(BEGIN_TX);
        realTransactionalFileStorage.beginTransaction(userIDAuth);
    }

    @Override
    public void txStoreDocument(UserIDAuth userIDAuth, DSDocument dsDocument) {
        inc(TX_STORE_DOCUMENT);
        realTransactionalFileStorage.txStoreDocument(userIDAuth, dsDocument);
    }

    @Override
    public DSDocument txReadDocument(UserIDAuth userIDAuth, DocumentFQN documentFQN) {
        inc(TX_READ_DOCUMENT);
        return realTransactionalFileStorage.txReadDocument(userIDAuth, documentFQN);
    }

    @Override
    public void txDeleteDocument(UserIDAuth userIDAuth, DocumentFQN documentFQN) {
        inc(TX_DELETE_DOCUMENT);
        realTransactionalFileStorage.txDeleteDocument(userIDAuth, documentFQN);
    }

    @Override
    public TxBucketContentFQN txListDocuments(UserIDAuth userIDAuth, DocumentDirectoryFQN documentDirectoryFQN, ListRecursiveFlag recursiveFlag) {
        inc(TX_LIST_DOCUMENTS);
        return realTransactionalFileStorage.txListDocuments(userIDAuth, documentDirectoryFQN, recursiveFlag);
    }

    @Override
    public TxDocumentFQNVersion getVersion(UserIDAuth userIDAuth, DocumentFQN documentFQN) {
        inc(GET_VERSION);
        return realTransactionalFileStorage.getVersion(userIDAuth, documentFQN);
    }

    @Override
    public boolean txDocumentExists(UserIDAuth userIDAuth, DocumentFQN documentFQN) {
        inc(TX_DOCUMENT_EXISTS);
        return realTransactionalFileStorage.txDocumentExists(userIDAuth, documentFQN);
    }

    @Override
    public void txDeleteFolder(UserIDAuth userIDAuth, DocumentDirectoryFQN documentDirectoryFQN) {
        inc(TX_DELETE_FOLDER);
        realTransactionalFileStorage.txDeleteFolder(userIDAuth, documentDirectoryFQN);
    }

    @Override
    public void endTransaction(UserIDAuth userIDAuth) {
        inc(END_TRANSACTION);
        realTransactionalFileStorage.endTransaction(userIDAuth);
    }

    @Override
    public void txMoveDocumentToInboxOfUser(UserIDAuth userIDAuth, UserID receiverUserID, DocumentFQN sourceDocumentFQN, DocumentFQN destDocumentFQN, MoveType moveType) {
        inc(TX_MOVE_DOCUMENT_TO_INBOX_OF_USER);
        realTransactionalFileStorage.txMoveDocumentToInboxOfUser(userIDAuth, receiverUserID, sourceDocumentFQN, destDocumentFQN, moveType);

    }

    @Override
    public DSDocument txMoveDocumentFromInbox(UserIDAuth userIDAuth, DocumentFQN source, DocumentFQN destination) {
        inc(TX_MOVE_FROM_INBOX);
        return realTransactionalFileStorage.txMoveDocumentFromInbox(userIDAuth, source, destination);
    }

    private void inc(String s) {
        if (!counterMap.containsKey(s)) {
            counterMap.put(s, new Integer(-1));
        }
        counterMap.put(s, counterMap.get(s) + 1);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("TransactionalDocumentSafeServiceTestWrapper{");
        List<String> keys = new ArrayList<>();
        keys.addAll(counterMap.keySet());

        Collections.sort(keys);
        keys.forEach(key -> {
            sb.append("\n");
            sb.append(fill(MAX, key));
            sb.append(" -> ");
            sb.append(counterMap.get(key));
        });

        sb.append("\n}");
        return sb.toString();
    }

    private String fill(int m, String s) {
        while(s.length() < m) {
            s = s + " ";
        }
        return s;
    }

}
