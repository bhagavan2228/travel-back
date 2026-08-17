package com.travelapp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private Weather weather = new Weather();
    private Assistant assistant = new Assistant();
    private Toxicity toxicity = new Toxicity();
    private Grok grok = new Grok();
    private Google google = new Google();

    @Data
    public static class Jwt {
        private String secret;
        private long accessTokenExpirationMs;
        private long refreshTokenExpirationMs;
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins;
    }

    @Data
    public static class Weather {
        private String apiKey;
        private String baseUrl;
    }

    @Data
    public static class Assistant {
        private String apiKey;
        private String baseUrl;
    }

    @Data
    public static class Toxicity {
        private String apiKey;
        private double threshold;
    }

    @Data
    public static class Grok {
        private String apiKey;
        private String baseUrl = "https://api.x.ai/v1";
        private String model = "grok-4.3";
    }

    @Data
    public static class Google {
        private String placesApiKey;
    }
}

