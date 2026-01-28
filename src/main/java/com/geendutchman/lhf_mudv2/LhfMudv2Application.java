package com.geendutchman.lhf_mudv2;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import com.geendutchman.lhf_mudv2.junction.DiscordJunction;
import com.geendutchman.lhf_mudv2.junction.StreamJunction;

@SpringBootApplication
public class LhfMudv2Application {

    public static void main(String[] args) {
        SpringApplication.run(LhfMudv2Application.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
            Logger logger = LoggerFactory.getLogger(getClass().getName() + ".beansLoader");
            logger.debug("What beans do we have today?");

            final String[] beanNames = ctx.getBeanDefinitionNames();
            Arrays.sort(beanNames);
            logger.atDebug().addKeyValue("beans", Arrays.toString(beanNames)).log("available beans");
            DiscordJunction djunction = new DiscordJunction();
            djunction.run();
            StreamJunction junction = new StreamJunction(System.console().reader(), System.console().writer());
            junction.run();
        };
    }

    @Bean
    public Duration timing() {
        return Duration.of(300, ChronoUnit.MILLIS);
    }

}
