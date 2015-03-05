package org.graylog.snmp;

import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.PluginModule;

import java.util.Arrays;
import java.util.Collection;

/**
 * Implement the Plugin interface here.
 */
public class SnmpPlugin implements Plugin {
    @Override
    public PluginMetaData metadata() {
        return new SnmpPluginMetaData();
    }

    @Override
    public Collection<PluginModule> modules () {
        return Arrays.<PluginModule>asList(new SnmpPluginModule());
    }
}
