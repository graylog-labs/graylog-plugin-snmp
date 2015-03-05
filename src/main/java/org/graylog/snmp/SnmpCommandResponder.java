package org.graylog.snmp;

import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.smi.TransportIpAddress;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;

public class SnmpCommandResponder implements CommandResponder {
    private static final Logger LOG = LoggerFactory.getLogger(SnmpCommandResponder.class);

    private final MessageInput input;

    public SnmpCommandResponder(final MessageInput input) {
        this.input = input;
    }

    @Override
    public void processPdu(final CommandResponderEvent event) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Processing SNMP event: {}", event);
        }

        final PDU pdu = event.getPDU();
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            pdu.encodeBER(outputStream);
        } catch (IOException e) {
            LOG.error("Error encoding PDU", e);
            return;
        }

        final TransportIpAddress peerAddress = (TransportIpAddress) event.getPeerAddress();
        final RawMessage rawMessage = new RawMessage(outputStream.toByteArray(), new InetSocketAddress(peerAddress.getInetAddress(), peerAddress.getPort()));

        input.processRawMessage(rawMessage);
    }
}
