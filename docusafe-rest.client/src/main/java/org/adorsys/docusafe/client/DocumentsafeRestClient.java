package org.adorsys.docusafe.client;


import org.adorsys.cryptoutils.utils.HexUtil;
import org.adorsys.docusafe.client.api.CreateUserRequest;
import org.adorsys.docusafe.client.api.DSDocument;
import org.adorsys.docusafe.client.api.ReadDocumentResponse;
import org.adorsys.docusafe.client.api.WriteDocumentRequest;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.stream.JsonGenerator;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;


/**
 * Created by peter on 27.02.18 at 09:19.
 */
public class DocumentsafeRestClient {
    private final static Logger LOGGER = LoggerFactory.getLogger(DocumentsafeRestClient.class);
    public static final int CHUNKSIZE = 1024;

    private String baseuri;
    private Client client;
    private static final String CREATE_USER = "internal/user";
    private static final String READ_DOCUMENT = "document";
    private static final String PASSWORD = "password";
    private static final String USER_ID = "userID";
    private static final String WRITE_DOCUMENT = "document";
    private static final String WRITE_DOCUMENT_STREAM1 = "document/stream1";

    public DocumentsafeRestClient(String baseuri) {
        this.baseuri = baseuri;
        client = ClientBuilder.newClient(new ClientConfig()
                // The line below that registers JSON-Processing feature can be
                // omitted if FEATURE_AUTO_DISCOVERY_DISABLE is not disabled.
                .property(JsonGenerator.PRETTY_PRINTING, true)
                .property(ClientProperties.REQUEST_ENTITY_PROCESSING, "CHUNKED")
                .property(ClientProperties.CHUNKED_ENCODING_SIZE, CHUNKSIZE)
        );
    }

    public void createUser(String userID, String password) {
        CreateUserRequest createUserRequest = new CreateUserRequest(userID, password);

        Response response = client.target(baseuri)
                .path(CREATE_USER)
                .request()
                .put(Entity.entity(createUserRequest, MediaType.APPLICATION_JSON_TYPE));
        LOGGER.info("User " + userID + "created: " + response.getStatus());
    }

    public void readDocument(String userID, String password, String fqn) {

        ReadDocumentResponse readDocument = client.target(baseuri)
                .path(READ_DOCUMENT)
                .path("\"" + fqn + "\"")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header(USER_ID, userID)
                .header(PASSWORD, password)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .get(ReadDocumentResponse.class);
        LOGGER.info("document " + fqn + " = " + readDocument.toString());

    }

    public void writeDocumentStream1(String userID, String password, String fqn, InputStream in, long size) {
        LOGGER.info("verschicke nun " + size + " bytes");
        String contentDisposition = "attachment; filename=\"" + fqn + "\"";
        Response response = client.target(baseuri)
                .path(WRITE_DOCUMENT_STREAM1)
                .request(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                .header("Content-Disposition", contentDisposition)
                .header(USER_ID, userID)
                .header(PASSWORD, password)
                .put(Entity.entity(in, MediaType.APPLICATION_OCTET_STREAM_TYPE));
    }

    public void writeDocument(String userID, String password, String fqn, byte[] data) {
        WriteDocumentRequest writeDocumentRequest = new WriteDocumentRequest();
        writeDocumentRequest.setDocumentFQN(fqn);
        writeDocumentRequest.setDocumentContent(HexUtil.convertBytesToHexString(data));
        DSDocument.DocumentMetaInfo documentMetaInfo = new DSDocument.DocumentMetaInfo();
        documentMetaInfo.setSize(data.length);
        writeDocumentRequest.setDsDocumentMetaInfo(documentMetaInfo);

        client.target(baseuri)
                .path(WRITE_DOCUMENT)
                .request()
                .header(USER_ID, userID)
                .header(PASSWORD, password)
                .put(Entity.entity(writeDocumentRequest, MediaType.APPLICATION_JSON_TYPE));

        LOGGER.info("document " + fqn + " verschickt");
    }


}