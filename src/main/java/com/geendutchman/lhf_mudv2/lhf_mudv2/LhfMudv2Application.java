package com.geendutchman.lhf_mudv2.lhf_mudv2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LhfMudv2Application {

    public static void main(String[] args) {
        System.setProperty("reactor.logging.fallback", "JDK");
        SpringApplication.run(LhfMudv2Application.class, args);
    }

}
