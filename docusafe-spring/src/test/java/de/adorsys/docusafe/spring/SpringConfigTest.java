package de.adorsys.docusafe.spring;

import de.adorsys.docusafe.business.DocumentSafeService;
import de.adorsys.docusafe.spring.annotation.UseDocusafeSpringConfiguration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(classes=SpringConfigTest.class)
@RunWith(SpringRunner.class)
@UseDocusafeSpringConfiguration
public class SpringConfigTest {

    @Autowired
    DocumentSafeService service;

    @Test
    public void a() {
        Assert.assertNotNull(service);
    }
}
