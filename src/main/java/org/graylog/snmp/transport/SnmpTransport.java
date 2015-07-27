package org.graylog.snmp.transport;

import com.codahale.metrics.MetricSet;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog.snmp.SnmpCommandResponder;
import org.graylog.snmp.oid.SnmpOIDDecoder;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.transports.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.SNMP4JSettings;
import org.snmp4j.Snmp;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.Priv3DES;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.transport.UdpTransportMapping;
import org.snmp4j.util.MultiThreadedMessageDispatcher;
import org.snmp4j.util.ThreadPool;

import java.io.IOException;

public class SnmpTransport implements Transport {
    private static final Logger LOG = LoggerFactory.getLogger(SnmpTransport.class);

    private static final String CK_MIBS_PATH = "mibs_path";
    private static final String CK_BIND_ADDRESS = "bind_address";
    private static final String CK_PORT = "port";

    private final int threadPoolSize;
    private final String bindAddress;
    private final int port;
    private UdpTransportMapping transport;

    @AssistedInject
    public SnmpTransport(@Assisted Configuration configuration, LocalMetricRegistry localMetricRegistry) {
        this.threadPoolSize = 1;
        this.bindAddress = configuration.getString(CK_BIND_ADDRESS);
        this.port = configuration.getInt(CK_PORT);

        SnmpOIDDecoder snmpOIDDecoder = new SnmpOIDDecoder(configuration.getString(CK_MIBS_PATH));
        SNMP4JSettings.setOIDTextFormat(snmpOIDDecoder);
    }

    @Override
    public void setMessageAggregator(CodecAggregator aggregator) {

    }

    @Override
    public void launch(MessageInput input) throws MisfireException {
        // Mostly stolen from https://github.com/javiroman/flume-snmp-source/blob/master/src/main/java/org/apache/flume/source/snmp/SNMPTrapSource.java

        final UdpAddress address = new UdpAddress(bindAddress + "/" + port);

        try {
            this.transport = new DefaultUdpTransportMapping(address);

            final ThreadPool threadPool = ThreadPool.create("snmp-transport-pool", threadPoolSize);
            final MultiThreadedMessageDispatcher dispatcher = new MultiThreadedMessageDispatcher(threadPool, new MessageDispatcherImpl());

            dispatcher.addMessageProcessingModel(new MPv1());
            dispatcher.addMessageProcessingModel(new MPv2c());
            dispatcher.addMessageProcessingModel(new MPv3());

            SecurityProtocols.getInstance().addDefaultProtocols();
            SecurityProtocols.getInstance().addPrivacyProtocol(new Priv3DES());

            /*
            final CommunityTarget target = new CommunityTarget();
            target.setCommunity(new OctetString("public"));
            */

            final Snmp snmp = new Snmp(dispatcher, transport);

            snmp.addCommandResponder(new SnmpCommandResponder(input));

            transport.listen();
        } catch (IOException e) {
            LOG.error("Error creating transport mapping", e);
        }
    }

    @Override
    public void stop() {
        try {
            transport.close();
        } catch (IOException e) {
            LOG.error("Closing transport failed", e);
        }
    }

    @Override
    public MetricSet getMetricSet() {
        return null;
    }

    @FactoryClass
    public interface Factory extends Transport.Factory<SnmpTransport> {
        SnmpTransport create(Configuration configuration);
    }


    @ConfigClass
    public static class Config implements Transport.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest cr = new ConfigurationRequest();

            cr.addField(
                    new TextField(
                            CK_MIBS_PATH,
                            "MIBs Path",
                            "",
                            "Custom MIBs load path in addition to the system defaults: /usr/share/mibs, /usr/share/snmp/mibs",
                            ConfigurationField.Optional.OPTIONAL
                    )
            );

            cr.addField(ConfigurationRequest.Templates.bindAddress(CK_BIND_ADDRESS));
            cr.addField(ConfigurationRequest.Templates.portNumber(CK_PORT, 1620));

            return cr;
        }
    }
}
