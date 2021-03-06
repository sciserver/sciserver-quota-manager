<h2>Welcome to SciServer's <a href='https://github.com/sciserver/sciserver-quota-manager'>Quota Manager</a> repo.</h2>

<h3>Table of Contents</h3>
<ul>
<li><a href="#description">Description</a></li>
<li><a href="#installation">Installation</a></li>
<li><a href="#usage">Usage</a></li>
<li><a href="#contributing">Contributing</a></li>
<li><a href="#license">License</a></li>
<li><a href="#contact">Contact</a></li>
</ul>

<h3 id="description">Description</h3>

This repository contains a utility for managing the quotas on a given system.

<h3 id="installation">Installation</h3>

<h4>Initial setup</h4>

Configuration is done by creating a yaml file somewhere that can be found at runtime. This can be set by using the `spring.config.additional-location` java property, `SPRING_CONFIG_ADDITIONAL_LOCATION` environmental variable, placing a yaml file in the working directory of the service, etc.

An `application.yaml` file can be placed on the root directory of this repository during development for use by IDEs. This will not be bundled into the compiled war file and is ignored by Git.

An example configuration can be found in `example-deployment/example-config.yaml`.

<h4>Common tasks</h4>

`./mvnw package` - resolves all dependencies, builds the project, and runs unit tests. War file is placed in `target`.

`mvnw package` - same as above, for Window developers

<h4>Eclipse setup</h4>

Many variants of Eclipse support maven projects, including the Eclipse IDE for Java Developers, Eclipse IDE for Java EE / Jakarta EE, Spring Tool Suite, etc. Spring Tool Suite is recommended.

In any of these variants of Eclipse, the following steps will import the project:

- Open the Import Wizard at File -> Import
- Select "Import an existing Maven project" if you have already cloned this repo
- Select "Check out a Maven project from SCM" to have Eclipse checkout this repo
- There is no need to run maven manually at any point. Eclipse will handle dependency management and reloading on configuration changes in the bundled `src/main/resources/application.properties`.

<h3 id="usage">Usage</h3>

Spring profiles are used to control the method of applying quotas. `xfs` is currently the default and only option, but to switch to another one (for example, if one is written is for Cephfs), then use the java `spring.profiles.active` option or `SPRING_PROFILES_ACTIVE` environmental variable.

This service will listen on port 21222 by default, but this is configurable with the `SERVER_PORT` environmental variable or the java `server.port` option.

To run in Spring Tool Suite, simply run as a Spring Boot App.
In other variants of Eclipse, running `QuotaManagerApplication` as a Java application will start a local web server and display the port in the Console.

To run standalone, the war file is runnable with `java -jar`. For example, to set the port to 222 use the `ceph` profile, and load /my/configuration.yaml, run:

    java -Dserver.port=222 -Dspring.profiles.active=ceph -Dspring.config.additional-location=/my/configuration.yaml -jar target/sciserver-quota-manager-0.0.1-SNAPSHOT.war

Alternatively, the war file itself is an executable script:

	SERVER_PORT=222 SPRING_PROFILES_ACTIVE=ceph SPRING_CONFIG_ADDITIONAL_LOCATION=/my/configuration.yaml ./target/sciserver-quota-manager-0.0.1-SNAPSHOT.war

The war file can also be deployed in any Servlet 3.0 container, including Tomcat 7.0+.

<h4 id="systemd-service">Running as a systemd service</h4>

The [example-deployment/sciserver-quota-manager.service](example-deployment/sciserver-quota-manager.service) file shows
an example of a locked-down service, assuming the war file is located at `/path/to/sciserver-quota-manager.war`, the
configuration is in a file at `/path/to/config.yaml`, and the managed folders are in `/path/to/storage`. Locking down
the service from system access is possible in systemd 232 and newer.

Instead of locking down the ability to write to the system, sciserver-quota-manager can be run as a regular user who has `sudo` access to the `xfs_quota` command and write access to the `/etc/project` and `/etc/projid` files.

<h4 id="authentication">Authentication</h4>

Authentication for almost all endpoints is via HTTP Basic authentication with a fixed username/password. By default, the username "user" and a random password printed in the logs is allowed. These can be set via the `spring.security.user.name` and `spring.security.user.password` respectively.

The only endpoints excluded from authentication are swagger-related (at `/swagger-ui.html`), the info actuator (at `/actuator/info`), and the health actuator (at `/actuator/health`). The health endpoint will only give an UP/DOWN message and appropriate status code when unauthorized, otherwise it will give details on the various health checks applied.

<h3 id="contributing">Contributing</h3>
After the first production release, this repo uses the branching strategies described in Git Flow.

<h3 id="license">License</h3>

SciServer File Service Manager is licensed under <a href="http://www.sciserver.org/docs/license/LICENCE.txt" target="_blank">Apache 2.0</a>.

<h3 id="contact">Contact</h3>

For technical questions or to report a bug, please email sciserver-helpdesk@jhu.edu.

For questions about collaborating with SciServer or using SciServer for Education, please contact <a href="mailto:sciserver-outreach@lists.johnshopkins.edu">sciserver-outreach@lists.johnshopkins.edu</a>.

For more information about SciServer, what it is, and what you can do with it, please visit www.sciserver.org.
