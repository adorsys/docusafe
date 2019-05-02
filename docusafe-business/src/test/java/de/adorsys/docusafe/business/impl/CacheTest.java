package de.adorsys.docusafe.business.impl;

import de.adorsys.dfs.connection.api.filesystem.FileSystemDFSConnection;
import de.adorsys.dfs.connection.api.service.api.DFSConnection;
import de.adorsys.dfs.connection.api.types.properties.ConnectionProperties;
import de.adorsys.dfs.connection.impl.factory.DFSConnectionFactory;
import de.adorsys.docusafe.business.DocumentSafeService;
import de.adorsys.docusafe.service.api.keystore.types.ReadKeyPassword;
import de.adorsys.docusafe.service.api.types.UserID;
import de.adorsys.docusafe.service.api.types.UserIDAuth;
import de.adorsys.docusafe.service.impl.keystore.service.KeyStoreServiceImpl;
import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.powermock.api.mockito.PowerMockito.spy;

@RunWith(value = PowerMockRunner.class)
@PowerMockIgnore("javax.*")
public class CacheTest {

    @SneakyThrows
    @Test
    public void createUserWithCache() {
        PowerMockito.mockStatic(DFSConnectionFactory.class);
        ArgumentCaptor<DFSConnectionFactory> props = ArgumentCaptor.forClass(DFSConnectionFactory.class);
        PowerMockito.verifyStatic(DFSConnectionFactory.class)
        DocumentSafeService service = new DocumentSafeServiceImpl(dfsConnection);
        PowerMockito.whenNew(FileSystemDFSConnection.class).withAnyArguments()
        PowerMockito.whenNew(FileSystemDFSConnection.class).withAnyArguments().thenReturn(keyStoreService);



        UserIDAuth userIDAuth = new UserIDAuth(new UserID("peter"), new ReadKeyPassword("affe"));
        service.createUser(userIDAuth);

        Mockito.verify(dfsConnection, Mockito.times(0)).putBlob(Mockito.any(), Mockito.any());

    }
}
