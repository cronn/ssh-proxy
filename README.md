[![CI](https://github.com/cronn/ssh-proxy/workflows/CI/badge.svg)](https://github.com/cronn/ssh-proxy/actions)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.cronn/ssh-proxy/badge.svg)](http://maven-badges.herokuapp.com/maven-central/de.cronn/ssh-proxy)
[![Apache 2.0](https://img.shields.io/github/license/cronn/ssh-proxy.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![codecov](https://codecov.io/gh/cronn/ssh-proxy/branch/main/graph/badge.svg?token=qhZPq2vKnv)](https://codecov.io/gh/cronn/ssh-proxy)
[![Valid Gradle Wrapper](https://github.com/cronn/ssh-proxy/workflows/Validate%20Gradle%20Wrapper/badge.svg)](https://github.com/cronn/ssh-proxy/actions/workflows/gradle-wrapper-validation.yml)

# SSH Proxy #

A pure Java implementation for SSH port tunneling that is able to understand
OpenSSH configurations which involve multiple hops to reach a target host.
This library essentially combines [JSch][jsch] with the ability to understand
`ProxyJump` or `ProxyCommand` configurations in your local `~/.ssh/config`
file.

See our [blog post "Tunneling Basics – Part II: OpenSSH Configuration Files"][blog-post-ssh-configuration-files] for
some context where and how this library can be used.

## Usage ##
Add the following Maven dependency to your project:

```xml
<dependency>
    <groupId>de.cronn</groupId>
    <artifactId>ssh-proxy</artifactId>
    <version>1.6</version>
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
    int randomLocalPort = sshProxy.connect("jumpHost2", "targetHost", targetPort);
    try (Socket s = new Socket(SshProxy.LOCALHOST, randomLocalPort)) {
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
[blog-post-ssh-configuration-files]: https://blog.cronn.de/en/ssh/configuration/2021/08/16/ssh-configuration.html
