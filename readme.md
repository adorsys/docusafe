# Document Safe

The document safe (docusafe) is a framework used to securely manage (store, share and retrieve) user documents on the top of a blob storage.

## Layer
The docusafe framework is a layer based software. Each layer depends on its underlying layer. The underlying layer does not have any dependencies to the layers above. 
The layers will be explained from bottom to top. 
* layer 0: **docusafe-service**

    This layer has dependencies to the project cryptutils (https://github.com/adorsys/cryptoutils). This framework contains the keystore handling, 
the en- and decryption and the StorecConnection. In the docusafe-service layer these funktionalties are joined, so that a document can be stored encrypted with a document guard.
This layer must not be used directly. Its services should be regarded as internal/private.
 
* layer 1: **docusafe-business**

    This layer provides the main functionality of the document safe framework. Users can be created. With these users documents can be created and stored. The documents can be saved as blobs or streams. 
The interface of this layer is ***DocumentSafeService.*** Just to give you an idea how easy it is to store and read a document here a simple tiny main.

```java
    package org.adorsys.docusafe.business;
    
    import org.adorsys.cryptoutils.exceptions.BaseException;
    import org.adorsys.cryptoutils.storeconnectionfactory.ExtendedStoreConnectionFactory;
    import org.adorsys.docusafe.business.impl.DocumentSafeServiceImpl;
    import org.adorsys.docusafe.service.api.types.UserID;
    import org.adorsys.docusafe.business.types.DSDocument;
    import org.adorsys.docusafe.business.types.DocumentFQN;
    import org.adorsys.docusafe.business.types.complex.UserIDAuth;
    import org.adorsys.docusafe.service.types.DocumentContent;
    import org.adorsys.encobject.domain.ReadKeyPassword;
    
    import java.util.Arrays;
    
    /**
     * Created by peter on 19.02.19 09:45.
     */
    public class ReadMeMDFileTestCode {
        public static void main(String[] args) {
            // create service
            DocumentSafeService documentSafeService = new DocumentSafeServiceImpl(ExtendedStoreConnectionFactory.get());
    
            // create user
            UserIDAuth userIDAuth = new UserIDAuth(new UserID("peter"), new ReadKeyPassword("passwordOfPeter"));
            documentSafeService.createUser(userIDAuth);
    
            // create document
            DocumentFQN documentFQN = new DocumentFQN("first/document.txt");
            DocumentContent documentContent = new DocumentContent(("programming is the mirror of your mind").getBytes());
            DSDocument dsDocument = new DSDocument(documentFQN, documentContent, null);
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
* layer 2: **docusafe-transactional**

    This layer provides the functionality to group actions that will be commited all together, or none of them. Now guess, you want store two documents and you get an exception 
for what every reason after storing the first but before storing the second document. The exception will raise and prevent the mandatory endTransaction(). So none of the documents 
will be stored at all. This layer gives you a handle to store your documents all the time consistently matching to each other.
This layer must not be used with layer1 at the same time! Documents stored with the services of layer1 can not be seen in layer2. If you use this layer, do not use services of layer1.
The interface of this layer is ***TransactionalDocumentSafeService.***
 
* layer 3: **docusafe-cached-transactional**

    This is a thin layer based upon the previous which prevents unneccesary payload when the same document is written or read more than one time. 
The interface of this layer is ***CachedTransactionalDocumentSafeService.*** And again, here a tiny how to use example

```java
package org.adorsys.docusafe.transactional;
    
import org.adorsys.cryptoutils.exceptions.BaseException;
import org.adorsys.cryptoutils.storeconnectionfactory.ExtendedStoreConnectionFactory;
import org.adorsys.docusafe.business.DocumentSafeService;
import org.adorsys.docusafe.business.impl.DocumentSafeServiceImpl;
import org.adorsys.docusafe.service.api.types.UserID;
import org.adorsys.docusafe.business.types.DSDocument;
import org.adorsys.docusafe.business.types.DocumentFQN;
import org.adorsys.docusafe.business.types.complex.UserIDAuth;
import org.adorsys.docusafe.cached.transactional.CachedTransactionalDocumentSafeService;
import org.adorsys.docusafe.cached.transactional.impl.*;
import org.adorsys.docusafe.cached.transactional.impl.SimpleRequestMemoryContextImpl;
import org.adorsys.docusafe.service.types.DocumentContent;
import org.adorsys.docusafe.transactional.impl.TransactionalDocumentSafeServiceImpl;
import org.adorsys.encobject.domain.ReadKeyPassword;
    
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by peter on 19.02.19 09:45.
 */
public class ReadMeMDFileTestCode {
    public static void main(String[] args) {
         // create service
        CachedTransactionalDocumentSafeService cachedTransactionalDocumentSafeService;
        {
            org.adorsys.docusafe.cached.transactional.impl.SimpleRequestMemoryContextImpl simpleRequestMemoryContext = new org.adorsys.docusafe.cached.transactional.impl.SimpleRequestMemoryContextImpl();
            DocumentSafeService documentSafeService = new DocumentSafeServiceImpl(ExtendedStoreConnectionFactory.get());
            TransactionalDocumentSafeService transactionalDocumentSafeService = new TransactionalDocumentSafeServiceImpl(simpleRequestMemoryContext, documentSafeService);
            cachedTransactionalDocumentSafeService = new CachedTransactionalDocumentSafeServiceImpl(simpleRequestMemoryContext, transactionalDocumentSafeService);
        }
    
        // create user
        UserIDAuth userIDAuth = new UserIDAuth(new UserID("peter"), new ReadKeyPassword("passwordOfPeter"));
        cachedTransactionalDocumentSafeService.createUser(userIDAuth);
    
        // begin Transaction
        cachedTransactionalDocumentSafeService.beginTransaction(userIDAuth);

        // create document
        DocumentFQN documentFQN = new DocumentFQN("first/document.txt");
        DocumentContent documentContent = new DocumentContent(("programming is the mirror of your mind").getBytes());
        DSDocument dsDocument = new DSDocument(documentFQN, documentContent, null);
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
    
* layer 4: **docusafe-spring**
    
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
