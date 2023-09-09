package com.thoainguyen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.thoainguyen.client")
@EnableCircuitBreaker
@EnableHystrix
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
