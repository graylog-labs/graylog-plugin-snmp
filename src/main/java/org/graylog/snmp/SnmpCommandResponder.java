package org.graylog.snmp;

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

public class SnmpCommandResponder implements CommandResponder {
    private static final Logger LOG = LoggerFactory.getLogger(SnmpCommandResponder.class);

    private static final String KEY_PREFIX = "snmp_";

    private final RawMessage rawMessage;
    private Message message = null;

    public SnmpCommandResponder(RawMessage rawMessage) {
        this.rawMessage = rawMessage;
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
        String decodedOid = oid.toString();
        if (decodedOid == null) {
            decodedOid = oid.toDottedString();
        }
        return decodedOid;
    }
}
