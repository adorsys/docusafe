package org.adorsys.documentsafe.layer01persistence.types.complextypes;

import org.adorsys.documentsafe.layer00common.exceptions.BaseException;
import org.adorsys.documentsafe.layer01persistence.exceptions.BucketException;
import org.adorsys.documentsafe.layer01persistence.types.BucketName;
import org.adorsys.encobject.domain.ObjectHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

/**
 * Created by peter on 16.01.18.
 * Die einzige Aufgabe des BucketPath ist es, für die LowLevel Speicheroperationen einen
 * ObjectHandle zur Verfügung zu stellen. Dieser wiederum besteht aus einem Container und
 * einem Namen. Dabei kann der Name auch Verzeichnisse haben. Auch kann es sein, dass es
 * keinen Container gibt!
 */
public class BucketPath {
    public final static String BUCKET_SEPARATOR = "/";
    private final static Logger LOGGER = LoggerFactory.getLogger(BucketPath.class);

    String container = null;
    String name = null;

    public BucketPath() {
    }

    /**
     * Wenn path einen Slash enthält, dann ist der Teil vor dem ersten Slash der Container und der Rest der Name
     * Wenn path keinen Slash enthält, dann ist alles der Container und der Name leer
     */
    public BucketPath(String path) {
        List<String> split = split(path);
        if (!split.isEmpty()) {
            container = split.remove(0);
            if (!split.isEmpty()) {
                name = split.stream().map(b -> b).collect(Collectors.joining(BucketName.BUCKET_SEPARATOR));
            }
        }
    }

    /**
     * container darf keinen Slash enthalten.
     * path darf slashes enthalten
     */
    public BucketPath(String container, String path) {
        if (container != null) {
            if (container.indexOf(BUCKET_SEPARATOR) != -1) {
                throw new BucketException("container " + container + " must not contain " + BUCKET_SEPARATOR);
            }
            this.container = container;
        }
        List<String> split = split(path);
        if (!split.isEmpty()) {
            name = split.stream().map(b -> b).collect(Collectors.joining(BucketName.BUCKET_SEPARATOR));
        }
    }

    public BucketPath(BucketPath bucketPath) {
        this.container = bucketPath.container;
        this.name = bucketPath.name;
    }

    /**
     * @return returns the new concatenated bucketPath
     * the BucketPath itself keeps untuched
     */
    public BucketPath append(BucketPath bucketPath) {
        String appendedName = "";
        if (name != null) {
            appendedName = name;
        }
        if (bucketPath.container != null) {
            if (appendedName.length() > 0) {
                appendedName += BUCKET_SEPARATOR;
            }
            appendedName += bucketPath.container;
        }
        if (bucketPath.name != null) {
            if (appendedName.length() > 0) {
                appendedName += BUCKET_SEPARATOR;
            }
            appendedName += bucketPath.name;
        }
        return new BucketPath(container, appendedName);
    }

    public BucketPath append(String path) {
        String appendedName = name;
        List<String> split = split(path);

        if (appendedName == null) {
            appendedName = split.stream().map(b -> b).collect(Collectors.joining(BucketName.BUCKET_SEPARATOR));
        } else {
            appendedName += BUCKET_SEPARATOR + split.stream().map(b -> b).collect(Collectors.joining(BucketName.BUCKET_SEPARATOR));
        }
        return new BucketPath(container, appendedName);
    }

    public BucketPath add(String suffix) {
        if (name == null) {
            throw new BucketException("add not possible, because name is null. container is " + container);
        }
        return new BucketPath(container, name + suffix);
    }

    public ObjectHandle getObjectHandle() {
        return new ObjectHandle(container, name);
    }

    /**
     * Separiert alle Elemente. Doppelte Slashes werden ignoriert.
     */
    private static List<String> split(String fullBucketPath) {
        List<String> list = new ArrayList<>();
        if (fullBucketPath == null) {
            return list;
        }
        StringTokenizer st = new StringTokenizer(fullBucketPath, BucketName.BUCKET_SEPARATOR);
        while (st.hasMoreElements()) {
            String token = st.nextToken();
            if (token.length() > 0) {
                list.add(token);
            }
        }
        return list;
    }


    @Override
    public String toString() {
        return "BucketPath{" + container + " - " + name + '}';
    }

    public BucketDirectory getBucketDirectory() {
        ObjectHandle objectHandle = getObjectHandle();
        String name = objectHandle.getName();
        if (name == null) {
            return new BucketDirectory("");
        }
        BucketDirectory documentDirectory = new BucketDirectory(this.getObjectHandle().getContainer());
        String directory = getDirectoryOf(name);
        if (directory != null) {
            documentDirectory = new BucketDirectory(documentDirectory.append(directory));
        }
        LOGGER.debug("directory for path : " + documentDirectory + " for " + this);
        return documentDirectory;
    }

    private static String getDirectoryOf(String value) {
        int i = value.lastIndexOf(BucketPath.BUCKET_SEPARATOR);
        if (i == -1) {
            return null;
        }
        return value.substring(0, i);
    }
}
