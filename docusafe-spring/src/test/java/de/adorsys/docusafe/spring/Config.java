package de.adorsys.docusafe.spring;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootConfiguration
@TestPropertySource(properties = {
        "docusafe.storeconnection.filesystem.rootbucket=target/test-filesystem",
        "some.bar.value=testValue"
})
public class Config {
}
