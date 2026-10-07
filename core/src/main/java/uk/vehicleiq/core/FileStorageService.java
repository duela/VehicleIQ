package uk.vehicleiq.core;

import java.nio.file.*;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.core.sync.RequestBody;

@Service
class FileStorageService {
    private final ObjectProvider<S3Client> s3Provider;
    @Value("${vehicleiq.upload-dir}") private String uploadDir;
    @Value("${vehicleiq.s3.bucket}") private String bucket;
    FileStorageService(ObjectProvider<S3Client> s3Provider){this.s3Provider=s3Provider;}
    String store(MultipartFile file)throws Exception{
        String key=UUID.randomUUID().toString();S3Client s3=s3Provider.getIfAvailable();
        if(s3!=null){
            try{s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());}
            catch(S3Exception missing){s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());}
            s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(file.getContentType()).build(),RequestBody.fromBytes(file.getBytes()));
        }else{
            Path root=Paths.get(uploadDir).toAbsolutePath().normalize();Files.createDirectories(root);Files.write(root.resolve(key),file.getBytes(),StandardOpenOption.CREATE_NEW);
        }
        return key;
    }
}

