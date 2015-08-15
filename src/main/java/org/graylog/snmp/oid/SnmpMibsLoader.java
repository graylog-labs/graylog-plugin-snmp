package org.graylog.snmp.oid;

import com.google.common.collect.ImmutableList;
import net.percederberg.mibble.Mib;
import net.percederberg.mibble.MibLoader;
import net.percederberg.mibble.MibLoaderException;
import net.percederberg.mibble.MibLoaderLog;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;

public class SnmpMibsLoader {
    private static final Logger LOG = LoggerFactory.getLogger(SnmpMibsLoader.class);
    private final List<Mib> allMibs;

    public SnmpMibsLoader(final String customMibsPath) {
        final MibLoader loader = new MibLoader();

        /* Order is important! First add all paths, then load MIBs from paths. */
        addMibsPath(loader, "/usr/share/mibs");
        addMibsPath(loader, "/usr/share/snmp/mibs");
        if (!isNullOrEmpty(customMibsPath)) {
            addMibsPath(loader, customMibsPath);
        }

        loadMibsFromPath(loader, "/usr/share/mibs");
        loadMibsFromPath(loader, "/usr/share/snmp/mibs");
        if (!isNullOrEmpty(customMibsPath)) {
            loadMibsFromPath(loader, customMibsPath);
        }

        this.allMibs = ImmutableList.copyOf(loader.getAllMibs());
    }

    public List<Mib> getAllMibs() {
        return allMibs;
    }

    private static void addMibsPath(MibLoader loader, String mibsPath){
        try {
            final File mibsDir = Paths.get(mibsPath).toAbsolutePath().toFile();

            if (mibsDir.isDirectory()) {
                loader.addAllDirs(mibsDir);
            } else {
                LOG.warn("Not a directory: {}", mibsDir);
            }
        } catch (Exception e) {
            LOG.error("Can not add MIBs path " + mibsPath);
        }
    }

    private static void loadMibsFromPath(MibLoader loader, String mibsPath) {
        final File mibsDir = Paths.get(mibsPath).toAbsolutePath().toFile();

        if (!mibsDir.isDirectory()) {
            LOG.warn("Not a directory: {}", mibsDir);
            return;
        }

        final Collection<File> mibFiles = FileUtils.listFiles(mibsDir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);

        if (mibFiles.isEmpty()) {
            LOG.warn("Can not find any MIB files in {}", mibsPath);
        }

        for (File file : mibFiles) {
            LOG.debug("Loading MIBs file: {}", file);
            try {
                if (file.isFile()) {
                    loader.load(file);
                }
            } catch (MibLoaderException e) {
                final StringBuilder builder = new StringBuilder();

                builder.append("Error parsing MIB file: ").append(file).append("\n");

                MibLoaderLog errorLog = e.getLog();
                Iterator errorItr = errorLog.entries();
                while (errorItr.hasNext()) {
                    MibLoaderLog.LogEntry element = (MibLoaderLog.LogEntry) errorItr.next();
                    builder.append(" - ").append(element.getMessage()).append("\n");
                }

                LOG.warn(builder.toString());
            } catch (Exception e) {
                LOG.error("Error loading MIB file: " + file.toString(), e);
            }
        }
    }
}
