README for FedICT Quick-Key Toolset Project
================================

=== 1. Introduction

This project contains the source code tree of the FedICT Quick-Key Toolset project.
The source code is hosted at: http://code.google.com/p/eid-quick-key-toolset/


=== 2. Requirements

The following is required for compiling the eID Applet and Engine software:
* Sun Java 1.6.0_16+
* Apache Maven 2.0.10/2.2.1

For running the compiled code under Unix/Mac OS systems, GPShell 1.4.2+ should be 
installed and 'gpshell' should be available in the command path.


=== 3. Build

The project can be build via:
	mvn clean install


=== 4. Eclipse IDE

The Eclipse project files can be created via:
	mvn eclipse:eclipse

Afterwards simply import the projects in Eclipse via:
	File -> Import... -> General:Existing Projects into Workspace

First time you use an Eclipse workspace you might need to add the maven 
repository location. Do this via:
    mvn eclipse:add-maven-repo -Declipse.workspace=<location of your workspace>


=== 5. NetBeans IDE

As of NetBeans version 6.7 this free IDE from Sun has native Maven 2 support.


=== 6. License

The license conditions can be found in the file: LICENSE.txt