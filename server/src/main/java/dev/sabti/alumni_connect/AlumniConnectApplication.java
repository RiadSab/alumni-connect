package dev.sabti.alumni_connect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// The employment nudge sweep runs on a cron.
@EnableScheduling
@SpringBootApplication
public class AlumniConnectApplication {

	public static void main(String[] args) {
		SpringApplication.run(AlumniConnectApplication.class, args);
	}

}
