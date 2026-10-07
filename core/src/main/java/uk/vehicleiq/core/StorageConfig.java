package uk.vehicleiq.core;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
class StorageConfig {
    @Bean @ConditionalOnProperty(name="vehicleiq.s3.enabled",havingValue="true")
    S3Client s3Client(@Value("${vehicleiq.s3.endpoint}") String endpoint,@Value("${vehicleiq.s3.region}") String region,
                      @Value("${vehicleiq.s3.access-key}") String access,@Value("${vehicleiq.s3.secret-key}") String secret){
        return S3Client.builder().endpointOverride(URI.create(endpoint)).region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(access,secret)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build()).build();
    }
}

