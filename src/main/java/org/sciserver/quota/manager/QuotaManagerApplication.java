/*******************************************************************************
 * Copyright 2018 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.sciserver.quota.manager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

import com.google.common.base.Predicates;

import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableSwagger2
public class QuotaManagerApplication {
	private static final String[] SWAGGER_ENDPOINTS = new String[] {
			"/v2/api-docs", "/configuration/ui", "/swagger-resources/**",
			"/configuration/**", "/swagger-ui.html", "/webjars/**"};

	public static void main(String[] args) {
		SpringApplication.run(QuotaManagerApplication.class, args);
	}

	@Bean
	public Docket api() {
		return new Docket(DocumentationType.SWAGGER_2)
				.select()
				.apis(RequestHandlerSelectors.any())
				.paths(Predicates.not(PathSelectors.ant("/error")))
				.build()
				.useDefaultResponseMessages(false);
	}

	@Bean WebSecurityConfigurerAdapter webSecurityConfigurerAdapter() {
		return new WebSecurityConfigurerAdapter() {
			@Override
			public void configure(HttpSecurity http) throws Exception {
				http
					.authorizeRequests()
						.antMatchers("/actuator/info", "/actuator/health").permitAll()
						.anyRequest().authenticated()
						.and()
					.httpBasic()
						.and()
					.exceptionHandling()
						.authenticationEntryPoint(new BasicAuthenticationEntryPoint());
			}

			@Override
			public void configure(WebSecurity web) {
				web
					.ignoring()
					.antMatchers(SWAGGER_ENDPOINTS);
			}
		};
	}
}
