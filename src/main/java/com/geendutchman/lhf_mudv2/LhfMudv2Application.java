package com.geendutchman.lhf_mudv2;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.logging.Logger;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class LhfMudv2Application {

    public static void main(String[] args) {
        System.setProperty("reactor.logging.fallback", "JDK");
        SpringApplication.run(LhfMudv2Application.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
            final Logger logger = Logger.getLogger(this.getClass().getName() + ".beansLoader");
            logger.finer("What beans do we have today?");

            final String[] beanNames = ctx.getBeanDefinitionNames();
            Arrays.sort(beanNames);
            for (String beanName : beanNames) {
                if (beanName.contains("spring")) {
                    logger.finest(beanName);
                } else {
                    logger.finer(beanName);
                }
            }
            logger.finer("Done listing beans");
        };
    }

    @Bean
    public Duration timing() {
        return Duration.of(300, ChronoUnit.MILLIS);
    }

}
