SNMP Plugin for Graylog
=======================

[![Build Status](https://travis-ci.org/Graylog2/graylog-plugin-snmp.svg)](https://travis-ci.org/Graylog2/graylog-plugin-snmp)

This plugin provides an input plugin to receive SNMP traps.

**Required Graylog version:** 2.0.0 and later

## Caveat

This plugin is still pretty young and hasn't seen production traffic yet. Please [let us know](/issues) if you see any problems. Thank you!

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
Please keep in mind that some MIBs need to be loaded with a special order. To achieve this create numbered sub-directories in your MIB load path and place the files there in the right order.
E.g. VMWare MIBs have to be loaded in this order:

```
   VMWARE-ROOT-MIB.mib 
   VMWARE-TC-MIB.mib 
   VMWARE-PRODUCTS-MIB.mib 
   VMWARE-SYSTEM-MIB.mib
   VMWARE-ENV-MIB.mib
   VMWARE-RESOURCES-MIB.mib
   VMWARE-VMINFO-MIB.mib
   ...
```

So you should create a directory structure like:

```
   /usr/share/mibs/1/VMWARE-ROOT-MIB.mib 
   /usr/share/mibs/2/VMWARE-TC-MIB.mib 
   /usr/share/mibs/3/VMWARE-PRODUCTS-MIB.mib
    ...
```

## Plugin Development

This project is using Maven 3 and requires Java 8 or higher.

* Clone this repository.
* Download [Mibble](http://www.mibble.org/download/index.html)
* `mvn install:install-file -DgroupId=net.percederberg -DartifactId=mibble-parser -Dversion=2.9.3 -Dpackaging=jar -Dfile=mibble-2.9.3/lib/mibble-parser-2.9.3.jar` (replace mibble version with the one you downloaded)
* `mvn install:install-file -DgroupId=net.percederberg -DartifactId=mibble-mibs -Dversion=2.9.3 -Dpackaging=jar -Dfile=mibble-2.9.3/lib/mibble-mibs-2.9.3.jar` (replace mibble version with the one you downloaded)
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

This sets the version numbers, creates a tag and pushes to GitHub. Travis CI will build the release artifacts and upload to GitHub automatically.
