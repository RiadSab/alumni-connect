package dev.sabti.alumni_connect.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "r2")
public class R2StorageService implements StorageService {

    private final S3Client s3;
    private final String bucket;

    public R2StorageService(
            @Value("${app.storage.r2.endpoint}") String endpoint,
            @Value("${app.storage.r2.bucket}") String bucket,
            @Value("${app.storage.r2.access-key}") String accessKey,
            @Value("${app.storage.r2.secret-key}") String secretKey) {
        this.bucket = bucket;
        // R2 ignores region (use "auto") and needs path-style access.
        this.s3 = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    @Override
    public String store(MultipartFile file) {
        String storageId = UUID.randomUUID().toString();
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(storageId)
                .contentType(file.getContentType())
                .build();
        try (InputStream in = file.getInputStream()) {
            s3.putObject(request, RequestBody.fromInputStream(in, file.getSize()));
        } catch (IOException e) {
            throw new StorageException("Failed to store file " + storageId, e);
        }
        return storageId;
    }

    @Override
    public String copy(String sourceStorageId) {
        String newStorageId = UUID.randomUUID().toString();
        s3.copyObject(CopyObjectRequest.builder()
                .sourceBucket(bucket).sourceKey(sourceStorageId)
                .destinationBucket(bucket).destinationKey(newStorageId)
                .build());
        return newStorageId;
    }

    // Files are small (<=5MB cap), so reading the whole object into memory keeps this simple and
    // gives the download a correct Content-Length, avoiding any streaming/content-length quirks.
    @Override
    public Resource loadAsResource(String storageId) {
        try {
            ResponseBytes<GetObjectResponse> object = s3.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucket).key(storageId).build());
            return new ByteArrayResource(object.asByteArray());
        } catch (NoSuchKeyException e) {
            throw new StorageException("File not found: " + storageId, e);
        }
    }

    @Override
    public void delete(String storageId) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(storageId).build());
    }
}
