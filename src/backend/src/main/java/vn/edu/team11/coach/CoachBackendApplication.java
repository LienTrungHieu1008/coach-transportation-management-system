package vn.edu.team11.coach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// Account authentication will be implemented from UC-P01/P02, without a generated demo account.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class CoachBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoachBackendApplication.class, args);
    }
}
