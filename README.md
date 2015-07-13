# Graylog SnmpPlugin Plugin

Welcome to your new Graylog plugin!

Please refer to http://docs.graylog.org/en/latest/pages/plugins.html for documentation on how to write
plugins for Graylog.


Getting started
---------------

This project is using Maven 3 and requires Java 7 or higher. The plugin will require Graylog 1.0.0 or higher.

* Clone this repository.
* Download [Mibble](http://www.mibble.org/download/index.html)
* `mvn install:install-file -DgroupId=net.percederberg -DartifactId=mibble-parser -Dversion=2.9.3 -Dpackaging=jar -Dfile=mibble-2.9.3/lib/mibble-parser-2.9.3.jar`
* `mvn install:install-file -DgroupId=net.percederberg -DartifactId=mibble-mibs -Dversion=2.9.3 -Dpackaging=jar -Dfile=mibble-2.9.3/lib/mibble-mibs-2.9.3.jar`
* Run `mvn package` to build a JAR file.
* Optional: Run `mvn jdeb:jdeb` and `mvn rpm:rpm` to create a DEB and RPM package respectively.
* Copy generated JAR file in target directory to your Graylog plugin directory.
* Install system packages 'snmp' and 'snmp-mibs-downloader'
* Copy additional MIB files to `/usr/share/mibs`
* Restart the Graylog.
