package com.azamzia.ingestion.adapter.s3;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Opens an S3 object as a streaming reader so files of any size use constant memory. */
@Component
public class S3FileSource {

    private final S3Client s3;

    public S3FileSource(S3Client s3) {
        this.s3 = s3;
    }

    public BufferedReader open(String bucket, String key) {
        GetObjectRequest request = GetObjectRequest.builder().bucket(bucket).key(key).build();
        ResponseInputStream<GetObjectResponse> stream = s3.getObject(request);
        return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
