[![Build Status](https://travis-ci.org/cronn-de/ssh-proxy.png?branch=master)](https://travis-ci.org/cronn-de/ssh-proxy)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.cronn/ssh-proxy/badge.svg)](http://maven-badges.herokuapp.com/maven-central/de.cronn/ssh-proxy)
[![Apache 2.0](https://img.shields.io/github/license/cronn-de/ssh-proxy.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Coverage Status](https://coveralls.io/repos/github/cronn-de/ssh-proxy/badge.svg?branch=master)](https://coveralls.io/github/cronn-de/ssh-proxy?branch=master)

# SSH Proxy #

A pure Java implementation for SSH port tunneling that is able to understand
OpenSSH configurations which involve multiple hops to reach a target host.
This library essentially combines [JSch][jsch] with the ability to understand
`ProxyJump` or `ProxyCommand` configurations in your local `~/.ssh/config`
file.

## Usage ##
Add the following Maven dependency to your project:

```xml
<dependency>
    <groupId>de.cronn</groupId>
    <artifactId>ssh-proxy</artifactId>
    <version>1.5</version>
</dependency>
```

### Example ###

```
# cat ~/.ssh/config

Host jumpHost1
    User my-user
    HostName jumphost1.my.domain

Host jumpHost2
    User other-user
    ProxyJump jumpHost1

Host targetHost
    ProxyCommand ssh -q -W %h:%p jumpHost2
```

```java
try (SshProxy sshProxy = new SshProxy()) {
    int targetPort = 1234;
    int port = sshProxy.connect("jumpHost2", "targetHost", targetPort);
    try (Socket s = new Socket(SshProxy.LOCALHOST, port)) {
        OutputStream out = s.getOutputStream();
        InputStream in = s.getInputStream();
        // ...
    }
}
```

## Dependencies ##

- Java 8+
- [JSch (with JZlib)][jsch]

[jsch]: http://www.jcraft.com/jsch/
