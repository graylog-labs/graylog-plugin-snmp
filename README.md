SNMP Plugin for Graylog
=======================

This plugin provides an input plugin to receive SNMP traps.

**Required Graylog version:** 1.0 and later

## Installation

[Download the plugin](https://github.com/Graylog2/graylog-plugin-snmp/releases)
and place the `.jar` file in your Graylog plugin directory. The plugin directory
is the `plugins/` folder relative from your `graylog-server` directory by default
and can be configured in your `graylog.conf` file.

Restart `graylog-server` and you are done.

## Setup

To get a basic set of MIB files on Linux you should install the `snmp` and `snmp-mibs-downloader` packages (the names might be different depending on the OS) and execute the `download-mibs` command. This fetches a lot of standard MIBs and installs them into the standard directories.

In the Graylog web interface, go to System/Inputs and create a new SNMP input like this:

![SNMP input creation dialog](https://github.com/Graylog2/graylog-plugin-snmp/blob/master/images/snmp-input-1.png)

Now you can point your SNMP devices to the configured IP address and port to receive SNMP traps.


### Custom MIBs

The input creation dialog allows you to configure a path to custom MIB files. Alternatively you can also copy your custom MIB files into the `/usr/share/mibs` directory which is included by default.

## Plugin Development

This project is using Maven 3 and requires Java 7 or higher.

* Clone this repository.
* Download [Mibble](http://www.mibble.org/download/index.html)
* `mvn install:install-file -DgroupId=net.percederberg -DartifactId=mibble-parser -Dversion=2.9.3 -Dpackaging=jar -Dfile=mibble-2.9.3/lib/mibble-parser-2.9.3.jar` (replace mibble version with the one you downloaded)
* Run `mvn package` to build a JAR file.
* Optional: Run `mvn jdeb:jdeb` and `mvn rpm:rpm` to create a DEB and RPM package respectively.
* Copy generated JAR file in target directory to your Graylog plugin directory.
* Install system packages 'snmp' and 'snmp-mibs-downloader'
* Execute 'sudo download-mibs'
* Copy additional MIB files to `/usr/share/mibs`
* Restart the Graylog.
* Send test trap `sudo snmptrap -v 2c -c public 127.0.0.1:1620 '' .1.3.6.1.4.1.5089.1.0.1 .1.3.6.1.4.1.5089.2.0.999 s "123456"`

## Plugin Release

We are using the maven release plugin:

```
$ mvn release:prepare
[...]
$ mvn release:perform
```

This sets the version numbers, creates a tag and pushes to GitHub. TravisCI will build the release artifacts and upload to GitHub automatically.
