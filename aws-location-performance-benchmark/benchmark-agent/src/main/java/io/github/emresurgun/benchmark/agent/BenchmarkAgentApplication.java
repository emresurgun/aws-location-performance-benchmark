package io.github.emresurgun.benchmark.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BenchmarkAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(BenchmarkAgentApplication.class, args);
    }

}
