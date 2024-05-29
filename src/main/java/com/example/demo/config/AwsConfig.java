package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.ssm.SsmClient;

@Configuration
public class AwsConfig {
    Region region = Region.AP_SOUTHEAST_1;

    @Bean
    public SecretsManagerClient secretsManagerClient() {
        return SecretsManagerClient.builder().region(region).build();
    }

    @Bean
    public SsmClient ssmClient() {
        return SsmClient.builder().region(region).build();
    }
}
