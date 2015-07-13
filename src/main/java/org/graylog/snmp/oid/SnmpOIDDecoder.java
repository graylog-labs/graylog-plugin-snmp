package org.graylog.snmp.oid;

import net.percederberg.mibble.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.util.OIDTextFormat;
import org.snmp4j.util.SimpleOIDTextFormat;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Iterator;
import java.util.List;

public class SnmpOIDDecoder implements OIDTextFormat {
    private static final Logger LOG = LoggerFactory.getLogger(SnmpOIDDecoder.class);
    private static MibLoader loader = loadMib(new File("/usr/share/mibs"));

    private static MibLoader loadMib(File mibsDir) {
        MibLoader loader = new MibLoader();
        loader.addAllDirs(mibsDir);
        File dir = new File(mibsDir.getAbsolutePath());
        List<File> mibFiles = (List<File>) FileUtils.listFiles(dir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        for (File file : mibFiles) {
            LOG.debug("Loading MIBs file: " + file.toString());
            try {
                if (new File(file.toString()).isFile()) {
                    loader.load(new File(file.toString()));
                }
            } catch (IOException e) {
                e.printStackTrace();
                LOG.error("Error loading MIB file: " + file.toString(), e);
            } catch (MibLoaderException e) {
                LOG.error("Error parsing MIB file: " + file.toString(), e);
                MibLoaderLog errorLog = e.getLog();
                Iterator errorItr = errorLog.entries();
                while (errorItr.hasNext()) {
                    MibLoaderLog.LogEntry element = (MibLoaderLog.LogEntry) errorItr.next();
                    LOG.error(element.getMessage());
                }
            }
        }
        return loader;
    }

    public static String findMibSymbol(String oid) {
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

    public static String formatOID(int[] value) {
        StringBuilder oid = new StringBuilder(3*value.length);
        for (int i=0; i<value.length; i++) {
            if (i != 0) {
                oid.append('.');
            }
            oid.append((value[i] & 0xFFFFFFFFL));
        }
        LOG.error("Received OID: " + oid.toString());
        return findMibSymbol(oid.toString());
    }

    @Override
    public String format(int[] value) {
        return SnmpOIDDecoder.formatOID(value);
    }

    @Override
    public String formatForRoundTrip(int[] value) { return format(value); }

    @Override
    public int[] parse(String text) throws ParseException {
        return SimpleOIDTextFormat.parseOID(text);
    }

}
