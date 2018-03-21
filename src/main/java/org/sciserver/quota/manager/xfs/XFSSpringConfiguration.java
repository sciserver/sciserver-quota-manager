package org.sciserver.quota.manager.xfs;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@Profile("xfs")
public class XFSSpringConfiguration {
	@Bean(name="xfsEditProjectsExecutor")
	public Executor xfsEditProjectsExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(1);
		executor.setMaxPoolSize(1);
		executor.setQueueCapacity(20);
		executor.setThreadNamePrefix("xfs-helper-");
		executor.initialize();
		return executor;
	}
}
