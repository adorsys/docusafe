package org.adorsys.docusafe.business.types;

import de.adorsys.common.basetypes.BaseTypeString;
import de.adorsys.common.exceptions.BaseException;
import de.adorsys.dfs.connection.api.complextypes.BucketDirectory;
import de.adorsys.dfs.connection.api.complextypes.BucketPath;

/**
 * Created by peter on 20.01.18 at 07:29.
 * Ist für den Benutzer der absolute Pfad einer Datei. Im System ist dieser
 * Pfad aber nur relativ, da noch der UserHomeBucketPath davor gesetzt werden muss.
 * Dieser Pfad beinhaltet auch schon den Dateinamen. Dieser ist einfach das
 * letzte Element im Pfad
 *
 * WICHTIG
 * Jeder DocumenteDirectoryFQN beginnt immer mit einem Slash, d.h. die Länge ist immer minimal 1
 */
public class DocumentDirectoryFQN extends BaseTypeString {
    public DocumentDirectoryFQN(String value) {
        super(DocumentFQN.check(prependStartingSeparator(value)));
    }

    public DocumentFQN addName(String value) {
        if (getValue().length() == 1) {
            return new DocumentFQN(value);
        }
        return addName(new DocumentFQN(value));
    }

    public DocumentFQN addName(DocumentFQN fqn) {
        if (getValue().length() == 1) {
            return fqn;
        }
        return new DocumentFQN(getValue() + fqn.getValue());
    }

    public DocumentDirectoryFQN addDirectory(String value) {
        if (getValue().length() == 1) {
            return new DocumentDirectoryFQN(value);
        }
        return addDirectory(new DocumentDirectoryFQN(value));
    }

    public DocumentDirectoryFQN addDirectory(DocumentDirectoryFQN value) {
        if (getValue().length() == 1) {
            return value;
        }
        if (value.getValue().length() == 1) {
            return new DocumentDirectoryFQN(getValue());
        }
        return new DocumentDirectoryFQN(getValue() + value.getValue());
    }

    public BucketDirectory prepend(BucketDirectory bucketDirectory) {
        if (getValue().length() == 1) {
            return bucketDirectory;
        }
        return bucketDirectory.appendDirectory(getValue());
    }

    private static String prependStartingSeparator(String s) {
        if (s.startsWith(BucketPath.BUCKET_SEPARATOR)) {
            if (s.length() > 1 && s.startsWith("//")) {
                throw new BaseException("programming error, path never should start with two dashes" + s);
            }
            return s;
        }
        return BucketPath.BUCKET_SEPARATOR + s;
    }
}
