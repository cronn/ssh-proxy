[![Build Status](https://travis-ci.org/cronn-de/ssh-proxy.png?branch=master)](https://travis-ci.org/cronn-de/ssh-proxy)

# SSH Proxy #

A pure Java implementation to tunnel to TCP endpoints through SSH.

## Usage ##
Add the following Maven dependency to your project:

```xml
<dependency>
    <groupId>de.cronn</groupId>
    <artifactId>ssh-proxy</artifactId>
    <version>1.0</version>
</dependency>
```

### Example ###

```java
try (SshProxy sshProxy = new SshProxy()) {
    int targetPort = 1234;
    int port = sshProxy.connect("jumpHost", "targetHost", targetPort);
    try (Socket s = new Socket(SshProxy.LOCALHOST, port)) {
        OutputStream out = s.getOutputStream();
        InputStream in = s.getInputStream();
        // ...
    }
}
```



## Dependencies ##

- Java 7+
- [JSch (with JZlib)][jsch]

[jsch]: http://www.jcraft.com/jsch/
