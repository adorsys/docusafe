# Document Safe

The document safe (docusafe) is a framework used to securely manage (read, write, update and delete) data on the top of a blob storage.

## Layers
The docusafe framework is a layer based software. 
Each layer depends on its underlying layer. 
The underlying layer does not have any dependencies to the layers above. 
The layers will be explained from bottom to top. 
* layer 0: **docusafe-service**

This layer does the encryption stuff. It is not to be uses by the clients. It provides a service
to create a keystore, to en- and decrypt a bucket path and to en- and decrypt a blob.
These services should be regarded as internal/private.
 
### layer 1: **docusafe-business**

This layer provides the main functionality of the document safe framework. 
Users can be created. With these users documents can be created and stored. 
The documents can be saved as blobs or streams. 
The interface of this layer is ***DocumentSafeService.*** Just to give you an idea how easy it is to store and read a document here a simple tiny main.

```java
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
```
### layer 2: **docusafe-transactional**

This layer provides the functionality to group actions that will be commited 
all together, or none of them. Now guess, you want store two documents and 
you get an exception for what every reason after storing the first but before storing the
second document. 
The exception will raise and prevent the mandatory endTransaction(). So none of the 
documents will be stored at all. This layer gives you a handle to store your documents 
all the time consistently matching to each other.
This layer must not be used with layer1 (docusafe-business) at the same time! 
Documents stored with the services of layer1 can not be seen in layer2. 
If you use this layer, do not use services of layer1.
The interface of this layer is ***TransactionalDocumentSafeService.***  
 
### layer 3: **docusafe-cached-transactional**

This is a thin layer based upon the previous which prevents unneccesary 
payload when the same document is written or read more than one time. 
The interface of this layer is ***CachedTransactionalDocumentSafeService.*** 
And again, here a tiny how to use example

```java
package de.adorsys.docusafe.transactional;

import de.adorsys.common.exceptions.BaseException;
import de.adorsys.dfs.connection.impl.factory.DFSConnectionFactory;
import de.adorsys.docusafe.business.DocumentSafeService;
import de.adorsys.docusafe.business.impl.DocumentSafeServiceImpl;
import de.adorsys.docusafe.service.api.keystore.types.ReadKeyPassword;
import de.adorsys.docusafe.service.api.types.DocumentContent;
import de.adorsys.docusafe.service.api.types.UserID;
import de.adorsys.docusafe.business.types.DSDocument;
import de.adorsys.docusafe.business.types.DocumentFQN;
import de.adorsys.docusafe.transactional.impl.TransactionalDocumentSafeServiceImpl;
import de.adorsys.docusafe.cached.transactional.CachedTransactionalDocumentSafeService;
import de.adorsys.docusafe.cached.transactional.impl.CachedTransactionalDocumentSafeServiceImpl;
import de.adorsys.docusafe.service.api.types.UserIDAuth;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by peter on 19.02.19 09:45.
 */
public class ReadMeMDFileTestCode {
    public static void main(String[] args) {
         // create service
        CachedTransactionalDocumentSafeService cachedTransactionalDocumentSafeService;
        {
            de.adorsys.docusafe.cached.transactional.impl.SimpleRequestMemoryContextImpl simpleRequestMemoryContext = new de.adorsys.docusafe.cached.transactional.impl.SimpleRequestMemoryContextImpl();
            DocumentSafeService documentSafeService = new DocumentSafeServiceImpl(DFSConnectionFactory.get());
            TransactionalDocumentSafeService transactionalDocumentSafeService = new TransactionalDocumentSafeServiceImpl(simpleRequestMemoryContext, documentSafeService);
            cachedTransactionalDocumentSafeService = new CachedTransactionalDocumentSafeServiceImpl(simpleRequestMemoryContext, transactionalDocumentSafeService, documentSafeService);
        }

        // create user
        UserIDAuth userIDAuth = new UserIDAuth(new UserID("peter"), new ReadKeyPassword("passwordOfPeter"));
        cachedTransactionalDocumentSafeService.createUser(userIDAuth);

        // begin Transaction
        cachedTransactionalDocumentSafeService.beginTransaction(userIDAuth);

        // create document
        DocumentFQN documentFQN = new DocumentFQN("first/document.txt");
        DocumentContent documentContent = new DocumentContent(("programming is the mirror of your mind").getBytes());
        DSDocument dsDocument = new DSDocument(documentFQN, documentContent);
        cachedTransactionalDocumentSafeService.txStoreDocument(userIDAuth, dsDocument);

        // read the document again
        DSDocument dsDocumentRead = cachedTransactionalDocumentSafeService.txReadDocument(userIDAuth, documentFQN);
        if (Arrays.equals(dsDocument.getDocumentContent().getValue(), dsDocumentRead.getDocumentContent().getValue()) == true) {
            System.out.println("read the following content from " + documentFQN + ":" + new String(dsDocumentRead.getDocumentContent().getValue()));
        } else {
            throw new BaseException("This will never happen :-)");
        }

        // end Transaction
        cachedTransactionalDocumentSafeService.endTransaction(userIDAuth);
    }

    public static class SimpleRequestMemoryContextImpl extends HashMap<Object, Object> {
    }
}
```
    
### layer 4: **docusafe-spring**
    
The layers 0-3 have to be used as direct services. This means, the classes have to be instantiated with new x-serviceImpl(). 
Specially the layer 2 and 3 need a MemoryContext object to store temporary information in memory 
rather than the filesystem.
As most server based architectures run 
with spring or ee context, there is no need to create your own implementation of a MemoryContext.
As the name of the layer implies,
it can be used with spring, to get the needed services to be injected for free.
This can be achieved by simply using the @UseDocusafeSpringConfiguration annotation. Then the ExtendedStoreConnection or the 
CachedTransactionalDocumentSafeService can be injected as autowired beans. Or, for more detailed access to the services, the
SpringExtendedStoreConnectionFactory or SpringCachedTransactionalDocusafeServiceFactory can be autowired.
 
## REST
If you are missing a REST layer, this is not provided by this framework. But in https://github.com/adorsys/docusafe.tests you can find a REST layer for the docusafe framework.
