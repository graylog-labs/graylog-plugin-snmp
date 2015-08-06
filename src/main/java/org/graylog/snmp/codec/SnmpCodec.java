package org.graylog.snmp.codec;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.annotations.Codec;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.AbstractCodec;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.PDU;
import org.snmp4j.asn1.BERInputStream;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.ByteBuffer;

@Codec(name = "snmp", displayName = "SNMP")
public class SnmpCodec extends AbstractCodec {
    private static final Logger LOG = LoggerFactory.getLogger(SnmpCodec.class);

    @AssistedInject
    protected SnmpCodec(@Assisted Configuration configuration, MetricRegistry metricRegistry) {
        super(configuration);
    }

    @Nullable
    @Override
    public Message decode(@Nonnull RawMessage rawMessage) {
        final PDU pdu = new PDU();

        try {
            pdu.decodeBER(new BERInputStream(ByteBuffer.wrap(rawMessage.getPayload())));
        } catch (IOException e) {
            LOG.error("Unable to decode SNMP PDU", e);
            return null;
        }

        final Message message = new Message("SNMP trap " + pdu.getRequestID().toString(), "source", rawMessage.getTimestamp());

        message.addField("trap_type", pdu.getType());
        message.addField("request_id", pdu.getRequestID().toLong());

        for (final VariableBinding binding : pdu.getVariableBindings()) {
            final String key = decodeOid(binding.getOid());
            final Variable variable = binding.getVariable();

            try {
                if (variable instanceof TimeTicks) {
                    message.addField(key, ((TimeTicks) variable).toMilliseconds());
                } else {
                    message.addField(key, variable.toLong());
                }
            } catch (UnsupportedOperationException e) {
                message.addField(key, variable.toString());
            }
        }

        return message;
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
            return super.getRequestedConfiguration();
        }

        @Override
        public void overrideDefaultValues(@Nonnull ConfigurationRequest cr) {
            super.overrideDefaultValues(cr);
        }
    }

    private String decodeOid(OID oid) {
        String decodedOid = oid.toString();
        if (decodedOid == null) {
            decodedOid = oid.toDottedString();
        }
        return decodedOid;
    }
}

