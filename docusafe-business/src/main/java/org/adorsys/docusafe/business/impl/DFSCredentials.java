package org.adorsys.docusafe.business.impl;

import de.adorsys.dfs.connection.api.filesystem.FilesystemConnectionPropertiesImpl;
import de.adorsys.dfs.connection.impl.amazons3.AmazonS3ConnectionProperitesImpl;
import lombok.Data;

@Data
public class DFSCredentials {
    FilesystemConnectionPropertiesImpl filesystem;
    AmazonS3ConnectionProperitesImpl amazons3;
}
