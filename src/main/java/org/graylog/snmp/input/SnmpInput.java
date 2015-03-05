package org.graylog.snmp.input;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog.snmp.codec.SnmpCodec;
import org.graylog.snmp.transport.SnmpTransport;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.FactoryClass;

import javax.inject.Inject;

public class SnmpInput extends MessageInput {
    private static final String NAME = "SNMP";

    @AssistedInject
    public SnmpInput(MetricRegistry metricRegistry,
                     @Assisted Configuration configuration,
                     SnmpTransport.Factory transportFactory,
                     LocalMetricRegistry localRegistry,
                     SnmpCodec.Factory codecFactory,
                     Config config,
                     Descriptor descriptor,
                     ServerStatus serverStatus) {
        super(metricRegistry,
                configuration,
                transportFactory.create(configuration),
                localRegistry,
                codecFactory.create(configuration),
                config, descriptor, serverStatus);
    }

    @FactoryClass
    public interface Factory extends MessageInput.Factory<SnmpInput> {
        @Override
        SnmpInput create(Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Descriptor extends MessageInput.Descriptor {
        @Inject
        protected Descriptor() {
            super(NAME, false, "");
        }
    }

    public static class Config extends MessageInput.Config {
        @Inject
        public Config(SnmpTransport.Factory transport, SnmpCodec.Factory codec) {
            super(transport.getConfig(), codec.getConfig());
        }
    }
}

