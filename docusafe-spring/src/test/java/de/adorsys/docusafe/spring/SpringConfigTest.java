package de.adorsys.docusafe.spring;

import de.adorsys.docusafe.business.DocumentSafeService;
import de.adorsys.docusafe.service.api.keystore.types.ReadKeyPassword;
import de.adorsys.docusafe.service.api.types.UserID;
import de.adorsys.docusafe.service.api.types.UserIDAuth;
import de.adorsys.docusafe.spring.annotation.UseDocusafeSpringConfiguration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootConfiguration
@UseDocusafeSpringConfiguration
public class SpringConfigTest {

    @Autowired
    DocumentSafeService service;

    @Test
    public void createAndDestroyUser() {
        Assert.assertNotNull(service);
        UserID userid = new UserID("peter");
        ReadKeyPassword password = new ReadKeyPassword("password");
        UserIDAuth userIDAuth = new UserIDAuth(userid, password);
        assert (!service.userExists(userid));
        service.createUser(userIDAuth);
        assert (service.userExists(userid));
        service.destroyUser(userIDAuth);

    }
}
