[![CI](https://github.com/cronn/ssh-proxy/workflows/CI/badge.svg)](https://github.com/cronn/ssh-proxy/actions)
[![Maven Central](https://img.shields.io/maven-central/v/de.cronn/ssh-proxy?logo=apache%20maven)](https://search.maven.org/#search|ga|1|g:de.cronn%20AND%20ssh-proxy)
[![Apache 2.0](https://img.shields.io/github/license/cronn/ssh-proxy.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![codecov](https://codecov.io/gh/cronn/ssh-proxy/branch/main/graph/badge.svg?token=qhZPq2vKnv)](https://codecov.io/gh/cronn/ssh-proxy)
[![Valid Gradle Wrapper](https://github.com/cronn/ssh-proxy/workflows/Validate%20Gradle%20Wrapper/badge.svg)](https://github.com/cronn/ssh-proxy/actions/workflows/gradle-wrapper-validation.yml)

# SSH Proxy #

> [!WARNING]
> **This project is no longer maintained.**
>
> SSH Proxy is built on the original [JSch][jsch], which is no longer maintained
> and lacks support for modern algorithms such as `rsa-sha2-256/512` or `ssh-ed25519`.
> [Apache MINA SSHD][mina-sshd] now covers the use case of this library out of the box:
> it reads `~/.ssh/config` (including multi-hop `ProxyJump`) and supports local port forwarding.
> We recommend migrating to Apache MINA SSHD. See [Migration to Apache MINA SSHD](#migration-to-apache-mina-sshd).

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

## Migration to Apache MINA SSHD ##

Add the dependency `org.apache.sshd:sshd-core`. The equivalent of the example above:

```java
import java.util.concurrent.TimeUnit;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.config.keys.ClientIdentity;
import org.apache.sshd.client.keyverifier.DefaultKnownHostsServerKeyVerifier;
import org.apache.sshd.client.keyverifier.RejectAllServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.util.net.SshdSocketAddress;

try (SshClient client = SshClient.setUpDefaultClient()) {
    // The defaults accept any server key and load no keys from ~/.ssh
    client.setServerKeyVerifier(new DefaultKnownHostsServerKeyVerifier(RejectAllServerKeyVerifier.INSTANCE));
    client.setKeyIdentityProvider(ClientIdentity.loadDefaultKeyPairProvider(true, true, FilePasswordProvider.EMPTY));
    client.start();

    // ProxyJump hops from ~/.ssh/config are resolved automatically
    try (ClientSession session = client.connect("jumpHost2").verify(10, TimeUnit.SECONDS).getClientSession()) {
        session.auth().verify(10, TimeUnit.SECONDS);
        int targetPort = 1234;
        SshdSocketAddress local = session.startLocalPortForwarding(
            new SshdSocketAddress("localhost", 0),
            new SshdSocketAddress("targetHost", targetPort));
        try (Socket s = new Socket("localhost", local.getPort())) {
            // ...
        }
    }
}
```

Differences to be aware of:

- `ProxyCommand` is not executed. Replace `ProxyCommand ssh -q -W %h:%p jumpHost` with `ProxyJump jumpHost`.
- Like OpenSSH, entries in `~/.ssh/known_hosts` for non-default ports must have the form `[host]:port`.
- In strict mode (first argument of `loadDefaultKeyPairProvider`), `~/.ssh` and the key files must not be accessible by other users.

## Dependencies ##

- Java 17+
- [JSch (with JZlib)][jsch]

[jsch]: http://www.jcraft.com/jsch/
[mina-sshd]: https://github.com/apache/mina-sshd
[blog-post-ssh-configuration-files]: https://blog.cronn.de/en/ssh/configuration/2021/08/16/ssh-configuration.html
