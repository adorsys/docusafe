package org.adorsys.documentsafe.layer03business.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.adorsys.documentsafe.layer02service.types.DocumentContent;
import org.adorsys.documentsafe.layer03business.types.complex.DSDocument;
import org.adorsys.documentsafe.layer03business.types.complex.DocumentFQN;
import org.adorsys.documentsafe.layer03business.types.complex.DocumentLink;

/**
 * Created by peter on 23.01.18 at 17:27.
 */
public class LinkUtil {
    public static DSDocument createDSDocument(DocumentLink documentLink, DocumentFQN documentFQN) {
        Gson gson = new GsonBuilder().create();
        String jsonString = gson.toJson(documentLink);
        DocumentContent documentContent = new DocumentContent(jsonString.getBytes());
        return new DSDocument(documentFQN, documentContent);
    }
}