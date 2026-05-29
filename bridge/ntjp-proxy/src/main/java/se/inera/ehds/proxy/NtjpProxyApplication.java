package se.inera.ehds.proxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import se.inera.ehds.proxy.config.ProxyProperties;

@SpringBootApplication
@EnableConfigurationProperties(ProxyProperties.class)
public class NtjpProxyApplication {
    public static void main(String[] args) {
        SpringApplication.run(NtjpProxyApplication.class, args);
    }
}
