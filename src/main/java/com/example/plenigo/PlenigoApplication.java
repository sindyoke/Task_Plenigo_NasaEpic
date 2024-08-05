package com.example.plenigo;

import com.example.plenigo.service.NasaEpicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.example.plenigo")
public class PlenigoApplication {

	@Autowired
	private NasaEpicService epicService;

	public static void main(String[] args) {
		SpringApplication.run(PlenigoApplication.class, args);
	}

	@Bean
	CommandLineRunner run() {
		return args -> {
			String date = args.length > 0 ? args[0] : null;
			String targetFolder = args.length > 1 ? args[1] : ".";
			epicService.downloadEpicImages(date, targetFolder);
		};
	}

}
