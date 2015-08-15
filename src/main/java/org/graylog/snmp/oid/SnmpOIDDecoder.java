package org.graylog.snmp.oid;

import net.percederberg.mibble.Mib;
import net.percederberg.mibble.MibValueSymbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.util.OIDTextFormat;
import org.snmp4j.util.SimpleOIDTextFormat;

import java.text.ParseException;
import java.util.List;

public class SnmpOIDDecoder implements OIDTextFormat {
    private static final Logger LOG = LoggerFactory.getLogger(SnmpOIDDecoder.class);

    private final SnmpMibsLoader loader;

    public SnmpOIDDecoder(SnmpMibsLoader mibsLoader) {
        this.loader = mibsLoader;
    }

    private String findMibSymbol(String oid) {
        final List<Mib> mibs = loader.getAllMibs();
        LOG.debug("Searching through " + String.valueOf(mibs.size()) + " MIBs");
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
