package com.azamzia.ingestion.config;

import com.azamzia.ingestion.core.CheckpointStore;
import com.azamzia.ingestion.core.ChunkedIngestor;
import com.azamzia.ingestion.core.RecordMapper;
import com.azamzia.ingestion.core.RecordSink;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.net.URI;

@Configuration
public class IngestionConfig {

    /** Uses the default AWS credentials chain; an endpoint override points the client at LocalStack. */
    @Bean
    S3Client s3Client(@Value("${ingestion.s3.region:us-east-1}") String region,
                      @Value("${ingestion.s3.endpoint:}") String endpoint) {
        S3ClientBuilder builder = S3Client.builder().region(Region.of(region));
        if (!endpoint.isBlank()) {
            builder = builder.endpointOverride(URI.create(endpoint)).forcePathStyle(true);
        }
        return builder.build();
    }

    @Bean
    ChunkedIngestor chunkedIngestor(RecordSink sink, CheckpointStore checkpoints,
                                    @Value("${ingestion.batch-size:1000}") int batchSize) {
        return new ChunkedIngestor(new RecordMapper(), sink, checkpoints, batchSize);
    }
}
