package org.sciserver.fileservice.manager;

import java.util.concurrent.Executor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@SpringBootApplication
@EnableAsync
public class SciserverFileserviceManagerApplication {
	public static void main(String[] args) {
		SpringApplication.run(SciserverFileserviceManagerApplication.class, args);
	}

	@Bean
	public Executor singleThreadExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(1);
		executor.setMaxPoolSize(1);
		executor.setQueueCapacity(20);
		executor.setThreadNamePrefix("singleThreadHelper-");
		executor.initialize();
		return executor;
	}
}
