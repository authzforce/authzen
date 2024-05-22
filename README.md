[![](https://img.shields.io/badge/tag-authzforce-orange.svg?logo=stackoverflow)](http://stackoverflow.com/questions/tagged/authzforce)
[![Docker badge](https://img.shields.io/docker/pulls/authzforce/authzen-pdp.svg)](https://hub.docker.com/r/authzforce/authzen-pdp/)
[![Build Status](https://github.com/authzforce/authzen/actions/workflows/maven.yml/badge.svg?branch=develop)](https://github.com/authzforce/authzen/actions/workflows/maven.yml)

# AuthzForce RESTful PDP server providing OpenID AuthZEN Access Evaluation API support

This is based on [AuthzForce RESTful PDP](http://github.com/authzforce/restful-pdp) with extensions to support OpenID AuthZEN API.

In particular, the project provides the following (Maven groupId:artifactId):
* `org.ow2.authzforce:authzforce-authzen-pdp-server`: a fully executable RESTful PDP server (runnable from the command-line), packaged as a [Spring Boot application](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment-install.html) or [Docker image](https://hub.docker.com/repository/docker/authzforce/authzen-pdp) (see the [Docker Compose example](pdp-server/docker) for usage) providing AuthZEN API.
* `org.ow2.authzforce:authzforce-authzen-pdp-extensions`: PDP extensions that can be plugged into [AuthzForce Core PDP](http://github.com/authzforce/core) engine to support AuthZEN JSON payloads (like XACML/JSON Profile's payloads), and that are used in the RESTful PDP server.

**(First release planned in June 2024!) Go to the [releases](https://github.com/authzforce/authzen/releases) page for
specific release info: downloads (Linux packages), Docker image,
[release notes](CHANGELOG.md)**

## System requirements
Java (JRE) 17 or later.

## Versions
See the [change log](CHANGELOG.md) following the *Keep a CHANGELOG* [conventions](http://keepachangelog.com/).

## License
See the [license file](LICENSE).

## Getting started

Launch the PDP with either Docker or the executable JAR as described in the next sections.

### Using Docker

Git clone this github repository or download the Source code ZIP from the [latest release](https://github.com/authzforce/authzen/releases) (**First release planned in June 2024!**) and unzip it, then from the git clone / unzipped folder, go to the [`docker`](pdp-server/docker) directory.

If you wish to use a different XACML Policy from the default, make sure your policy file is in the `pdp/conf/policies` folder and set the `rootPolicyRef` to that policy ID in the PDP configuration file `pdp/conf/pdp.xml`.

Then run: `docker compose up -d`, then `docker compose logs` to check the PDP is up and running.

(You can change the logging verbosity by modifying the Logback configuration file `pdp/conf/logback.xml`.)

### Using the executable JAR

Get the latest release's **(First release planned in June 2024!)** [executable JAR](https://repo1.maven.org/maven2/org/ow2/authzforce/authzforce-authzen-pdp-server/) from Maven Central with groupId/artifactId = `org.ow2.authzforce`/`authzforce-authzen-pdp-server`; or build it from the source by running `mvn install` (the JAR will be located in `pdp-server/target` folder). The name of the JAR is `authzforce-authzen-pdp-server-M.m.p.jar` (replace `M.m.p` with the latest version).

Make sure it is executable (replace `M.m.p` with the current version):

```sh
chmod u+x authzforce-authzen-pdp-server-M.m.p.jar
```

Copy the content of [that folder](pdp-server/docker/pdp/conf) to the same directory.

If you wish to use a different XACML Policy from the default, make sure your policy file is in the `policies` folder and set the `rootPolicyRef` to that policy ID in the PDP configuration file `pdp.xml`.

Then run the executable from that directory as follows (replace `M.m.p` with the current version):

```sh
$ ./authzforce-authzen-pdp-server-M.m.p.jar
```

If it refuses to start because the TCP listening port is already used (by some other server on the system), you can change that port in file `application.yml` copied previously: uncomment and change `server.port` property value to something else (default is 8080).

You know the embedded server is up and running when you see something like this (if and only if the logger for Spring classes is at least in INFO level, according to Logback configuration file mentioned down below) :
```
... Tomcat started on port(s): 8080 (http)
```

(You can change the logging verbosity by modifying the Logback configuration file `logback.xml` copied previously.)

### Send a request to the PDP

Once the PDP is up and running, you can make a request from a different terminal, for example using a request from one of folders in [AuthZEN Interop test directory](pdp-extensions/src/test/resources/AuthZEN_Identiverse_2024_Interop) (install `curl` tool if you don't have it already on your system):

```sh
$ curl --include --header "Content-Type: application/json" --data @/path/to/request.json http://localhost:8080/access/v1/evaluation
```
*Add --verbose option for more details.*
You should get a JSON response such as:

```
{"decision": true}
```


## Extensions
If you are missing features in AuthzForce, you can extend it with various types of plugins (without changing the existing code) the same way you do for the RESTful PDP, so please refer to the [RESTful PDP project's README](https://github.com/authzforce/restful-pdp?tab=readme-ov-file#extensions) for the instructions. 

## Vulnerability reporting
If you want to report a vulnerability, please follow the [GitHub procedure for private vulnerability reporting](https://docs.github.com/en/code-security/security-advisories/guidance-on-reporting-and-writing-information-about-vulnerabilities/privately-reporting-a-security-vulnerability#privately-reporting-a-security-vulnerability).

## Support
If you are experiencing any issue with this project except for vulnerabilities mentioned previously, please report it on the [GitHub Issue Tracker](https://github.com/authzforce/authzen/issues).
Please include as much information as possible; the more we know, the better the chance of a quicker resolution:

* Software version
* Platform (OS and JDK)
* Stack traces generally really help! If in doubt include the whole thing; often exceptions get wrapped in other exceptions and the exception right near the bottom explains the actual error, not the first few lines at the top. It's very easy for us to skim-read past unnecessary parts of a stack trace.
* Log output can be useful too; sometimes enabling DEBUG logging can help;
* Your code & configuration files are often useful.

If you wish to contact the developers for other reasons, use [AuthzForce contact mailing list](http://scr.im/azteam).

## Contributing
See [CONTRIBUTING.md](CONTRIBUTING.md).

