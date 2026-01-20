package com.group17.lilyoutube_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class LilyoutubeServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(LilyoutubeServerApplication.class, args);
	}

}
