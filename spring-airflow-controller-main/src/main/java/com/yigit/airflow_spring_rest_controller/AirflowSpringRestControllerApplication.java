package com.yigit.airflow_spring_rest_controller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.config.EnableWebFlux;

@SpringBootApplication
@EnableWebFlux
@EnableScheduling
public class AirflowSpringRestControllerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AirflowSpringRestControllerApplication.class, args);
	}

}
