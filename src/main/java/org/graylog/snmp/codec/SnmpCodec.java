package org.graylog.snmp.codec;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog.snmp.SnmpCommandResponder;
import org.graylog.snmp.oid.SnmpMibsLoaderRegistry;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.annotations.Codec;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.AbstractCodec;
import org.graylog2.plugin.inputs.transports.NettyTransport;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.MessageDispatcher;
import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.TransportStateReference;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.OctetString;
import org.snmp4j.transport.DummyTransport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;

@Codec(name = "snmp", displayName = "SNMP")
public class SnmpCodec extends AbstractCodec {
    private static final Logger LOG = LoggerFactory.getLogger(SnmpCodec.class);

    public static final String CK_MIBS_PATH = "mibs_path";

    private final SnmpMibsLoaderRegistry mibsLoaderRegistry;
    private final String mibsPath;

    @AssistedInject
    protected SnmpCodec(@Assisted Configuration configuration,
                        MetricRegistry metricRegistry,
                        SnmpMibsLoaderRegistry mibsLoaderRegistry) {
        super(configuration);
        this.mibsPath = configuration.getString(CK_MIBS_PATH);
        this.mibsLoaderRegistry = mibsLoaderRegistry;
    }

    @Nullable
    @Override
    public Message decode(@Nonnull RawMessage rawMessage) {
        try {
            final MessageDispatcher messageDispatcher = new MessageDispatcherImpl();
            final SnmpCommandResponder responder = new SnmpCommandResponder(rawMessage, mibsLoaderRegistry, mibsPath);

            final USM usm = new USM(SecurityProtocols.getInstance(), new OctetString(MPv3.createLocalEngineID()), 0);
            usm.setEngineDiscoveryEnabled(true);

            messageDispatcher.addCommandResponder(responder);
            messageDispatcher.addMessageProcessingModel(new MPv1());
            messageDispatcher.addMessageProcessingModel(new MPv2c());
            messageDispatcher.addMessageProcessingModel(new MPv3(usm));

            final IpAddress ipAddress = new IpAddress(rawMessage.getRemoteAddress().getAddress());
            final DummyTransport<IpAddress> transport = new DummyTransport<>(ipAddress);

            messageDispatcher.processMessage(
                    transport,
                    ipAddress,
                    ByteBuffer.wrap(rawMessage.getPayload()),
                    new TransportStateReference(transport, ipAddress, null, SecurityLevel.undefined, SecurityLevel.undefined, false, null)
            );

            return responder.getMessage();
        } catch (Exception e) {
            LOG.error("Unable to decode SNMP packet", e);
            return null;
        }
    }

    @FactoryClass
    public interface Factory extends AbstractCodec.Factory<SnmpCodec> {
        @Override
        SnmpCodec create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends AbstractCodec.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest cr = super.getRequestedConfiguration();
            cr.addField(
                    new TextField(
                            CK_MIBS_PATH,
                            "MIBs Path",
                            "",
                            "Custom MIBs load path in addition to the system defaults: /usr/share/mibs, /usr/share/snmp/mibs",
                            ConfigurationField.Optional.OPTIONAL
                    )
            );
            return cr;
        }

        @Override
        public void overrideDefaultValues(@Nonnull ConfigurationRequest cr) {
            super.overrideDefaultValues(cr);

            if (cr.containsField(NettyTransport.CK_PORT)) {
                cr.getField(NettyTransport.CK_PORT).setDefaultValue(1620);
            }
        }
    }

}

