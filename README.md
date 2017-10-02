[![Build Status](https://travis-ci.org/cronn-de/ssh-proxy.png?branch=master)](https://travis-ci.org/cronn-de/ssh-proxy)

# SSH Proxy #

A pure Java implementation to tunnel to TCP endpoints through SSH. It is an
abstraction above [JSch][jsch] that is able to understand more sophisticated
OpenSSH configurations which involve multiple hops to reach a target host.

## Usage ##
Add the following Maven dependency to your project:

```xml
<dependency>
    <groupId>de.cronn</groupId>
    <artifactId>ssh-proxy</artifactId>
    <version>1.1</version>
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
    ProxyCommand ssh -q -W %h:%p jumpHost1

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
