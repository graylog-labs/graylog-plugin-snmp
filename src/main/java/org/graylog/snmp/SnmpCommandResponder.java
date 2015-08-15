package org.graylog.snmp;

import com.google.common.collect.Iterables;
import org.graylog.snmp.oid.SnmpMibsLoader;
import org.graylog.snmp.oid.SnmpOIDDecoder;
import org.graylog.snmp.oid.SnmpMibsLoaderRegistry;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.util.OIDTextFormat;

public class SnmpCommandResponder implements CommandResponder {
    private static final Logger LOG = LoggerFactory.getLogger(SnmpCommandResponder.class);

    private static final String KEY_PREFIX = "snmp_";

    private final RawMessage rawMessage;
    private final OIDTextFormat oidTextFormat;
    private Message message = null;

    public SnmpCommandResponder(RawMessage rawMessage, SnmpMibsLoaderRegistry mibsLoaderRegistry, String mibsPath) {
        this.rawMessage = rawMessage;

        final String inputId = Iterables.getLast(rawMessage.getSourceNodes()).inputId;
        final SnmpMibsLoader mibsLoader = mibsLoaderRegistry.get(inputId);

        if (mibsLoader == null) {
            LOG.info("Initialize new SnmpMibsLoader (custom path: \"{}\")", mibsPath);
            mibsLoaderRegistry.put(inputId, new SnmpMibsLoader(mibsPath));
            this.oidTextFormat = new SnmpOIDDecoder(mibsLoaderRegistry.get(inputId));
        } else {
            this.oidTextFormat = new SnmpOIDDecoder(mibsLoader);
        }
    }

    public Message getMessage() {
        return message;
    }

    private String withKeyPrefix(String key) {
        return KEY_PREFIX + key;
    }

    @Override
    public void processPdu(CommandResponderEvent event) {
        LOG.debug("Processing SNMP event: {}", event);

        final PDU pdu = event.getPDU();
        final Integer32 requestID = pdu.getRequestID();
        final Message message = new Message("SNMP trap " + requestID.toString(), null, rawMessage.getTimestamp());

        message.addField(withKeyPrefix("trap_type"), PDU.getTypeString(pdu.getType()));
        message.addField(withKeyPrefix("request_id"), requestID.toLong());

        for (final VariableBinding binding : pdu.getVariableBindings()) {
            final String key = decodeOid(binding.getOid());
            final Variable variable = binding.getVariable();

            try {
                if (variable instanceof TimeTicks) {
                    message.addField(withKeyPrefix(key), ((TimeTicks) variable).toMilliseconds());
                } else {
                    message.addField(withKeyPrefix(key), variable.toLong());
                }
            } catch (UnsupportedOperationException e) {
                message.addField(withKeyPrefix(key), variable.toString());
            }
        }

        this.message = message;
    }

    private String decodeOid(OID oid) {
        final String decodedOid = oidTextFormat.formatForRoundTrip(oid.getValue());

        if (decodedOid != null) {
            return decodedOid;
        }

        return oid.toDottedString();
    }
}
