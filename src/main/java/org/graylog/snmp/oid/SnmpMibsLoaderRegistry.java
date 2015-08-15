package org.graylog.snmp.oid;

import com.google.common.collect.Maps;

import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentMap;

public class SnmpMibsLoaderRegistry {
    private final ConcurrentMap<String, SnmpMibsLoader> registry = Maps.newConcurrentMap();

    public void put(final String id, final SnmpMibsLoader mibsLoader) {
        registry.putIfAbsent(id, mibsLoader);
    }

    @Nullable
    public SnmpMibsLoader get(final String id) {
        return registry.get(id);
    }

    public void remove(String id) {
        registry.remove(id);
    }
}
