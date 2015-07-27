package org.graylog.snmp.oid;

import net.percederberg.mibble.Mib;
import net.percederberg.mibble.MibLoader;
import net.percederberg.mibble.MibLoaderException;
import net.percederberg.mibble.MibLoaderLog;
import net.percederberg.mibble.MibValueSymbol;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.util.OIDTextFormat;
import org.snmp4j.util.SimpleOIDTextFormat;

import java.io.File;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Collection;
import java.util.Iterator;

import static com.google.common.base.Strings.isNullOrEmpty;

public class SnmpOIDDecoder implements OIDTextFormat {
    private static final Logger LOG = LoggerFactory.getLogger(SnmpOIDDecoder.class);

    private final MibLoader loader = new MibLoader();

    public SnmpOIDDecoder(String customMibsPath) {
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

                builder.append("Error parsing MIB file: " + file.toString() + "\n");

                MibLoaderLog errorLog = e.getLog();
                Iterator errorItr = errorLog.entries();
                while (errorItr.hasNext()) {
                    MibLoaderLog.LogEntry element = (MibLoaderLog.LogEntry) errorItr.next();
                    builder.append(" - " + element.getMessage() + "\n");
                }

                LOG.warn(builder.toString());
            } catch (Exception e) {
                LOG.error("Error loading MIB file: " + file.toString(), e);
            }
        }
    }

    private String findMibSymbol(String oid) {
        Mib[] mibs = loader.getAllMibs();
        LOG.debug("Searching through " + String.valueOf(mibs.length) + " MIBs");
        String name = null;
        for (Mib mib : mibs) {
            MibValueSymbol symbol = mib.getSymbolByOid(oid);
            if (symbol == null) {
                continue;
            }
            name = symbol.getName();
            LOG.debug("Found symbol: " + name);
        }
        return name;
    }

    private String formatOID(int[] value) {
        StringBuilder oid = new StringBuilder(3*value.length);
        for (int i=0; i<value.length; i++) {
            if (i != 0) {
                oid.append('.');
            }
            oid.append((value[i] & 0xFFFFFFFFL));
        }
        LOG.debug("Received OID: " + oid.toString());
        return findMibSymbol(oid.toString());
    }

    @Override
    public String format(int[] value) {
        return formatOID(value);
    }

    @Override
    public String formatForRoundTrip(int[] value) { return format(value); }

    @Override
    public int[] parse(String text) throws ParseException {
        return SimpleOIDTextFormat.parseOID(text);
    }

}
