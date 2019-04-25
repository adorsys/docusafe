package org.adorsys.docusafe.service.impl.cmsencryption.services;


import de.adorsys.common.exceptions.BaseExceptionHandler;
import de.adorsys.dfs.connection.api.domain.Payload;
import de.adorsys.dfs.connection.api.service.impl.SimplePayloadImpl;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.adorsys.docusafe.service.api.cmsencryption.CMSEncryptionService;
import org.adorsys.docusafe.service.api.exceptions.DecryptionException;
import org.adorsys.docusafe.service.api.keystore.types.KeyID;
import org.adorsys.docusafe.service.api.keystore.types.KeyStoreAccess;
import org.adorsys.docusafe.service.impl.cmsencryption.exceptions.AsymmetricEncryptionException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OutputEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.io.*;
import java.security.*;
import java.util.Iterator;

/**
 * Cryptographic message syntax document encoder/decoder - see
 *
 * @see <a href=https://en.wikipedia.org/wiki/Cryptographic_Message_Syntax">CMS wiki</a>
 */
@Slf4j
public class CMSEncryptionServiceImpl implements CMSEncryptionService {

    public CMSEncryptionServiceImpl() {
    }

    @Override
    public CMSEnvelopedData encrypt(Payload payload, PublicKey publicKey, KeyID publicKeyId) {
        try {
            CMSEnvelopedDataGenerator cmsEnvelopedDataGenerator = new CMSEnvelopedDataGenerator();
            JceKeyTransRecipientInfoGenerator jceKey = new JceKeyTransRecipientInfoGenerator(
                    publicKeyId.getValue().getBytes(),
                    publicKey
            );

            cmsEnvelopedDataGenerator.addRecipientInfoGenerator(jceKey);
            CMSTypedData msg = new CMSProcessableByteArray(payload.getData());
            OutputEncryptor encryptor = new JceCMSContentEncryptorBuilder(CMSAlgorithm.AES128_CBC)
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build();
            return cmsEnvelopedDataGenerator.generate(msg, encryptor);
        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }
    }

    @Override
    public Payload decrypt(CMSEnvelopedData cmsEnvelopedData, KeyStoreAccess keyStoreAccess) {
        try {

            RecipientInformationStore recipients = cmsEnvelopedData.getRecipientInfos();

            Iterator<RecipientInformation> recipientInformationIterator = recipients.getRecipients().iterator();
            if (!recipientInformationIterator.hasNext()) {
                throw new AsymmetricEncryptionException("CMS Envelope doesn't contain recipients");
            }
            RecipientInformation recipientInfo = recipientInformationIterator.next();
            if (recipientInformationIterator.hasNext()) {
                throw new AsymmetricEncryptionException("PROGRAMMING ERROR. HANDLE OF MORE THAN ONE RECIPIENT NOT DONE YET");
            }
            KeyTransRecipientId recipientId = (KeyTransRecipientId) recipientInfo.getRID();
            byte[] subjectKeyIdentifier = recipientId.getSubjectKeyIdentifier();
            String keyId = new String(subjectKeyIdentifier);
            log.debug("Private key ID from envelope: {}", keyId);

            PrivateKey privateKey = (PrivateKey) keyStoreAccess.getKeyStore().getKey(
                    keyId,
                    keyStoreAccess.getKeyStoreAuth().getReadKeyPassword().getValue().toCharArray()
            );

            JceKeyTransRecipient recipient = new JceKeyTransEnvelopedRecipient(privateKey);

            return new SimplePayloadImpl(recipientInfo.getContent(recipient));
        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }
    }

    @Override
    public InputStream buildEncryptionInputStream(InputStream is, PublicKey publicKey, KeyID publicKeyID) {
        try {
            File file = File.createTempFile("fos-encrypted-", "");
            try (FileOutputStream fos = new MyFileOutputStream(file)) {
                RecipientInfoGenerator rec = new JceKeyTransRecipientInfoGenerator(publicKeyID.getValue().getBytes(), publicKey);
                readFromInputStreamAndWriteToOutputStream(is, fos, rec);
                return new MyFileInputStream(file);
            }
        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }
    }

    @Override
    public InputStream buildEncryptionInputStream(InputStream is, SecretKey secretKey, KeyID keyID) {
        try {
            File file = File.createTempFile("fos-encrypted-", "");
            try (FileOutputStream fos = new MyFileOutputStream(file)) {
                RecipientInfoGenerator rec = new JceKEKRecipientInfoGenerator(keyID.getValue().getBytes(), secretKey);
                readFromInputStreamAndWriteToOutputStream(is, fos, rec);
            }
            return new MyFileInputStream(file);
        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }
    }

    @Override
    @SneakyThrows
    public InputStream buildDecryptionInputStream(InputStream inputStream, KeyStoreAccess keyStoreAccess) {
        try {
            RecipientInformationStore recipientInfoStore = new CMSEnvelopedDataParser(inputStream).getRecipientInfos();

            if (recipientInfoStore.size() == 0) {
                throw new DecryptionException("CMS Envelope doesn't contain recipients");
            }
            if (recipientInfoStore.size() > 1) {
                throw new DecryptionException("Programming error. Handling of more that one recipient not done yet");
            }
            RecipientInformation recipientInfo = recipientInfoStore.getRecipients().stream().findFirst().get();
            RecipientId rid = recipientInfo.getRID();

            switch (rid.getType()) {
                case RecipientId.keyTrans:
                    return recipientInfo.getContentStream(new JceKeyTransEnvelopedRecipient(privateKey(keyStoreAccess, rid)))
                            .getContentStream();
                case RecipientId.kek:
                    return recipientInfo.getContentStream(new JceKEKEnvelopedRecipient(secretKey(keyStoreAccess, rid)))
                            .getContentStream();
                default:
                    throw new DecryptionException("Programming error. Handling of more that one recipient not done yet");
            }
        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }
    }

    private SecretKey secretKey(KeyStoreAccess keyStoreAccess, RecipientId rid)
            throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        String keyIdentifier = new String(((KEKRecipientId) rid).getKeyIdentifier());
        log.debug("Secret key ID from envelope: {}", keyIdentifier);
        return (SecretKey) keyStoreAccess.getKeyStore().getKey(keyIdentifier,
                keyStoreAccess.getKeyStoreAuth().getReadKeyPassword().getValue().toCharArray());
    }

    private PrivateKey privateKey(KeyStoreAccess keyStoreAccess, RecipientId rid)
            throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        String subjectKeyIdentifier = new String(((KeyTransRecipientId) rid).getSubjectKeyIdentifier());
        log.debug("Private key ID from envelope: {}", subjectKeyIdentifier);
        return (PrivateKey) keyStoreAccess.getKeyStore().getKey(subjectKeyIdentifier,
                keyStoreAccess.getKeyStoreAuth().getReadKeyPassword().getValue().toCharArray());
    }

    private OutputStream streamEncrypt(OutputStream dataContentStream, RecipientInfoGenerator rec)
            throws CMSException, IOException {
        CMSEnvelopedDataStreamGenerator gen = new CMSEnvelopedDataStreamGenerator();
        gen.addRecipientInfoGenerator(rec);
        return gen.open(dataContentStream, new JceCMSContentEncryptorBuilder(CMSAlgorithm.AES128_CBC)
                .setProvider(BouncyCastleProvider.PROVIDER_NAME).build());
    }

    private void readFromInputStreamAndWriteToOutputStream(InputStream is, FileOutputStream fos, RecipientInfoGenerator rec) {
        try {
            OutputStream os = streamEncrypt(fos, rec);

            int content;
            byte[] buffer = new byte[8 * 1024]; //8kb
            while ((content = is.read(buffer)) != -1) {
                os.write(buffer, 0, content);
            }
            os.flush();
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(fos);
        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }
    }

    public static class MyFileInputStream extends FileInputStream {
        private final static Logger LOGGER = LoggerFactory.getLogger(MyFileInputStream.class);
        File file = null;
        boolean hereItBecomeseUgly = false;

        public MyFileInputStream(File file) throws FileNotFoundException {
            super(file);
            this.file = file;
            LOGGER.debug("temp fis " + file);
        }

        /**
         * Please do not change unless you test this under windows. The fis can not be deleted unless the super.close is
         * completly done. The hereItBecomesUgly is neccessary to avoid calling delete too early.
         */
        @Override
        public void close() {
            if (hereItBecomeseUgly) {
                return;
            }
            try {
                LOGGER.debug("close fis " + file);
                hereItBecomeseUgly = true;
                super.close();
                hereItBecomeseUgly = false;
                LOGGER.debug("closed fis " + file);
                delete();
            } catch (Exception e) {
                BaseExceptionHandler.handle(e);
            }
        }

        public void delete() {
            try {
                if (file != null) {
                    LOGGER.debug("delete file " + file);
                    FileUtils.forceDelete(file);
                    LOGGER.debug("deleted file " + file);
                    file = null; // to avoid reentrance
                }
            } catch (Exception e) {
                BaseExceptionHandler.handle(e);
            }
        }
    }

    public static class MyFileOutputStream extends FileOutputStream {
        private final static Logger LOGGER = LoggerFactory.getLogger(MyFileOutputStream.class);
        File file = null;

        public MyFileOutputStream(File file) throws FileNotFoundException {
            super(file);
            this.file = file;
            LOGGER.debug("temp fos is " + file);
        }

        @Override
        public void close() {
            try {
                LOGGER.debug("close fos " + file);
                super.close();
            } catch (Exception e) {
                BaseExceptionHandler.handle(e);
            }
        }
    }


}
