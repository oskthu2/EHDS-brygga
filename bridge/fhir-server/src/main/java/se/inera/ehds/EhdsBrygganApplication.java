package se.inera.ehds;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class EhdsBrygganApplication {
    public static void main(String[] args) {
        SpringApplication.run(EhdsBrygganApplication.class, args);
    }
}
