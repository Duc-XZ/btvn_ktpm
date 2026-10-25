package vn.edu.demo.benchmark;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;

import java.util.Map;

@SpringBootApplication
public class BenchmarkRunnerApplication {
    public static void main(String[] args) {
        System.setProperty("logging.level.root", "OFF");
        System.setProperty("debug", "false");
        SpringApplication application = new SpringApplication(BenchmarkRunnerApplication.class);
        application.setBannerMode(Banner.Mode.OFF);
        application.setLogStartupInfo(false);
        application.setDefaultProperties(Map.of("logging.level.root", "OFF", "debug", "false"));
        application.run(args);
    }
}
