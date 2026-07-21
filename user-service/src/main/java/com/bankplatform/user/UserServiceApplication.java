package com.bankplatform.user;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        // Run in UTC regardless of host locale — avoids environment-dependent bugs
        // (e.g. the JDBC driver rejecting locale-specific zone aliases at connect time).
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
