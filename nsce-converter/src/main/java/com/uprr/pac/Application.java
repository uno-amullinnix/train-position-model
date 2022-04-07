package com.uprr.pac;


import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
@Profile("import")
public class Application implements CommandLineRunner {
    public static void main(String[] args) throws Exception {
	SpringApplication.run(Application.class, args);
    }

    public void run(String... args) throws Exception {
	 log.info("EXECUTING : command line runner");
	 
	        for (int i = 0; i < args.length; ++i) {
	            log.info("args[{}]: {}", i, args[i]);
	        }
	
    }
}
