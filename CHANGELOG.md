Graylog SNMP Plugin Changes
===========================

## 0.3.0 (2015-08-15)

* Refactor SNMP message handling to avoid issues. (#3)
* Rename `SnmpInput` class to `SnmpUDPInput`.
  **WARNING**: You have to re-create your SNMP inputs because of the class
  name change!
* All SNMP fields in the message are now prefixed with `snmp_` to avoid
  conflicts with other fields.
