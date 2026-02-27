package com.example.oa.workflow.service;

import com.example.oa.common.BizException;
import com.example.oa.common.ResultCode;
import com.example.oa.workflow.config.MinioConfig.MinioProperties;
import com.example.oa.workflow.entity.OaAttachment;
import com.example.oa.workflow.mapper.OaAttachmentMapper;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 附件上传：写入 MinIO 并落库 oa_attachment。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final OaAttachmentMapper attachmentMapper;

    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024L; // 20MB
    private static final String PATH_PREFIX = "attachments/";

    /**
     * 上传文件。businessType 为 leave 或 expense，businessId 可为空（提交申请后再关联）。
     */
    public OaAttachment upload(MultipartFile file, String businessType, Long uploadUserId) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "请选择文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException(ResultCode.BAD_REQUEST, "文件大小不能超过 20MB");
        }
        ensureBucket();

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "file";
        }
        String objectKey = PATH_PREFIX + uploadUserId + "/" + LocalDateTime.now().toLocalDate() + "/"
                + UUID.randomUUID().toString().replace("-", "") + "-" + sanitizeFileName(originalFilename);

        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectKey)
                            .stream(is, file.getSize(), -1)
                            .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                            .build());
        } catch (Exception e) {
            log.warn("MinIO putObject failed", e);
            throw new BizException(ResultCode.INTERNAL_ERROR, "上传失败: " + e.getMessage());
        }

        OaAttachment att = new OaAttachment();
        att.setBusinessType(businessType != null ? businessType : "leave");
        att.setBusinessId(null);
        att.setFileName(originalFilename);
        att.setFilePath(objectKey);
        att.setFileSize(file.getSize());
        att.setFileContentType(file.getContentType());
        att.setUploadUserId(uploadUserId);
        attachmentMapper.insert(att);
        return att;
    }

    /** 提交申请后可选：将附件关联到业务主键 */
    public void linkToBusiness(String businessType, String businessId, java.util.List<Long> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty()) return;
        for (Long id : attachmentIds) {
            OaAttachment att = attachmentMapper.selectById(id);
            if (att != null && businessType.equals(att.getBusinessType())) {
                att.setBusinessId(businessId);
                attachmentMapper.updateById(att);
            }
        }
    }

    private void ensureBucket() {
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioProperties.getBucket()).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioProperties.getBucket()).build());
            }
        } catch (Exception e) {
            log.warn("MinIO ensureBucket failed", e);
            throw new BizException(ResultCode.INTERNAL_ERROR, "存储桶检查失败: " + e.getMessage());
        }
    }

    private static String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
