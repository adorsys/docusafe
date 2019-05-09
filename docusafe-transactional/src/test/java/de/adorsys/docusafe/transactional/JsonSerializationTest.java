package de.adorsys.docusafe.transactional;

import de.adorsys.docusafe.service.api.types.DocumentContent;
import de.adorsys.docusafe.transactional.impl.TxIDHashMapWrapper;
import de.adorsys.docusafe.transactional.impl.helper.Class2JsonHelper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class JsonSerializationTest {
    @Test
    public void test() {
        TxIDHashMapWrapper w = new TxIDHashMapWrapper();
        Class2JsonHelper h = new Class2JsonHelper();
        DocumentContent documentContent = h.txidHashMapToContent(w);
        String s = new String(documentContent.getValue());
        log.info(s);

        TxIDHashMapWrapper w2 = h.txidHashMapFromContent(documentContent);
        log.info(w.toString());
        log.info(w2.toString());
    }
}
