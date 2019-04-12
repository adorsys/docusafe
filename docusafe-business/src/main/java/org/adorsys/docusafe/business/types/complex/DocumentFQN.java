package org.adorsys.docusafe.business.types.complex;

import de.adorsys.common.basetypes.BaseTypeString;
import de.adorsys.common.exceptions.BaseException;
import de.adorsys.dfs.connection.api.complextypes.BucketPath;

/**
 * Created by peter on 20.01.18 at 07:29.
 * Ist für den Benutzer der absolute Pfad einer Datei. Im System ist dieser
 * Pfad aber nur relativ, da noch der UserHomeBucketPath davor gesetzt werden muss.
 * Dieser Pfad beinhaltet auch schon den Dateinamen. Dieser ist einfach das
 * letzte Element im Pfad
 */
public class DocumentFQN extends BaseTypeString {
    public DocumentFQN(String value) {
        super(check(prependStartingSeparator(value)));
    }

    public DocumentDirectoryFQN getDocumentDirectory() {
        String value = getValue();
        int lastIndex = value.lastIndexOf(BucketPath.BUCKET_SEPARATOR);
        return new DocumentDirectoryFQN(value.substring(0, lastIndex));
    }

    // Nur der Name, dennoch beginnt dieser auch mit einem Slash
    public DocumentFQN getPlainNameWithoutPath() {
        String fqn = getValue();
        return new DocumentFQN(fqn.substring(fqn.lastIndexOf(BucketPath.BUCKET_SEPARATOR )));
    }

    private static String prependStartingSeparator(String s) {
        if (s.startsWith(BucketPath.BUCKET_SEPARATOR)) {
            return s;
        }
        return BucketPath.BUCKET_SEPARATOR + s;
    }

    public static String check(String s) {
        if (s.length() > 1 && s.endsWith(BucketPath.BUCKET_SEPARATOR)) {
            throw new BaseException(s + " must not end with a slash");
        }
        if (s.contains("//")) {
            throw new BaseException(s + " must not contain dobule slashes");
        }
        return s;
    }

}
