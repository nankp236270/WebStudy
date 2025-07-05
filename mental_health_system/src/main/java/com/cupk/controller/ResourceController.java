package com.cupk.controller;

import com.cupk.entity.TextResource;
import com.cupk.entity.VideoResource;
import com.cupk.service.TextResourceService;
import com.cupk.service.VideoResourceService;
import com.cupk.service.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/resource")
public class ResourceController {
    private static final Logger logger = LoggerFactory.getLogger(ResourceController.class);

    @Autowired
    private VideoResourceService videoResourceService;

    @Autowired
    private TextResourceService textResourceService;

    @Value("${upload.dir:src/main/resources/static/upload/}")
    private String uploadDir;

    // 推荐资源接口

    @GetMapping("/recommend")
    public Result<Map<String, Object>> recommendResources(@RequestParam Long userId) {
        // 直接返回所有视频和文案资源，按顺序一一对应
        List<VideoResource> videos = videoResourceService.list();
        List<TextResource> texts = textResourceService.list();
        Map<String, Object> data = new HashMap<>();
        data.put("videos", videos);
        data.put("texts", texts);
        return Result.success(data);
    }

    private static class ResourceRecommendation {
        private List<VideoResource> videos;
        private List<TextResource> texts;

        public ResourceRecommendation(List<VideoResource> videos, List<TextResource> texts) {
            this.videos = videos;
            this.texts = texts;
        }

        public List<VideoResource> getVideos() {
            return videos;
        }

        public List<TextResource> getTexts() {
            return texts;
        }
    }

    private String extractEmotionTagFromAssessment(String report) {
        if (report == null || report.isEmpty()) {
            return "default";
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(report);

            JsonNode riskLevelNode = root.get("risk_level");
            if (riskLevelNode != null && riskLevelNode.isTextual()) {
                String riskLevel = riskLevelNode.asText();
                switch (riskLevel) {
                    case "绿色":
                        return "low_risk";
                    case "黄色":
                        return "medium_risk";
                    case "红色":
                        return "high_risk";
                    default:
                        return "default";
                }
            } else {
                return "default";
            }

        } catch (IOException e) {
            e.printStackTrace();
            return "default";
        }
    }

    // ========================= 管理接口 =========================

    @GetMapping("/videos")
    public Result<List<VideoResource>> listVideos() {
        logger.info("获取所有视频资源");
        List<VideoResource> videos = videoResourceService.list();
        logger.info("返回 {} 个视频资源", videos.size());
        return Result.success(videos);
    }

    @PostMapping("/videos")
    public Result<?> addVideo(@RequestBody VideoResource videoResource) {
        logger.info("添加视频资源: {}", videoResource.getTitle());

        // 设置上传时间
        videoResource.setUploadTime(LocalDateTime.now());

        // 如果没有设置情绪标签，设置默认值
        if (videoResource.getEmotionTag() == null || videoResource.getEmotionTag().isEmpty()) {
            videoResource.setEmotionTag("default");
        }

        // 保存视频资源
        try {
            videoResourceService.save(videoResource);
            logger.info("视频资源添加成功，ID: {}", videoResource.getId());
            return Result.success();
        } catch (Exception e) {
            logger.error("视频资源添加失败: {}", e.getMessage(), e);
            return Result.error("添加失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/videos/{id}")
    public Result<?> deleteVideo(@PathVariable Long id) {
        logger.info("删除视频资源，ID: {}", id);
        boolean success = videoResourceService.removeById(id);
        logger.info("删除视频资源 {} {}", id, success ? "成功" : "失败");
        return success ? Result.success() : Result.error("删除失败");
    }

    @PutMapping("/videos")
    public Result<?> updateVideo(@RequestBody VideoResource videoResource) {
        logger.info("更新视频资源，ID: {}", videoResource.getId());
        boolean success = videoResourceService.updateById(videoResource);
        logger.info("更新视频资源 {} {}", videoResource.getId(), success ? "成功" : "失败");
        return success ? Result.success() : Result.error("更新失败");
    }

    @GetMapping("/texts")
    public Result<List<TextResource>> listTexts() {
        logger.info("获取所有文案资源");
        List<TextResource> texts = textResourceService.list();
        logger.info("返回 {} 个文案资源", texts.size());
        return Result.success(texts);
    }

    @PostMapping("/texts")
    public Result<?> addText(@RequestBody TextResource textResource) {
        logger.info("添加文案资源: {}", textResource.getTitle());

        // 设置创建时间
        textResource.setCreateTime(LocalDateTime.now());

        // 如果没有设置情绪标签，设置默认值
        if (textResource.getEmotionTag() == null || textResource.getEmotionTag().isEmpty()) {
            textResource.setEmotionTag("default");
        }

        // 保存文案资源
        try {
            textResourceService.save(textResource);
            logger.info("文案资源添加成功，ID: {}", textResource.getId());
            return Result.success();
        } catch (Exception e) {
            logger.error("文案资源添加失败: {}", e.getMessage(), e);
            return Result.error("添加失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/texts/{id}")
    public Result<?> deleteText(@PathVariable Long id) {
        logger.info("删除文案资源，ID: {}", id);
        boolean success = textResourceService.removeById(id);
        logger.info("删除文案资源 {} {}", id, success ? "成功" : "失败");
        return success ? Result.success() : Result.error("删除失败");
    }

    @PutMapping("/texts")
    public Result<?> updateText(@RequestBody TextResource textResource) {
        logger.info("更新文案资源，ID: {}", textResource.getId());
        textResource.setUpdateTime(LocalDateTime.now());
        boolean success = textResourceService.updateById(textResource);
        logger.info("更新文案资源 {} {}", textResource.getId(), success ? "成功" : "失败");
        return success ? Result.success() : Result.error("更新失败");
    }

    @GetMapping("/image-info/{filename:.+}")
    public Result<?> getImageInfo(@PathVariable String filename) {
        File uploadDirectory = new File(uploadDir);
        File imageFile = new File(uploadDirectory, filename);

        Map<String, Object> info = new HashMap<>();
        info.put("filename", filename);
        info.put("exists", imageFile.exists());
        info.put("size", imageFile.exists() ? imageFile.length() : -1);
        info.put("path", imageFile.getAbsolutePath());
        info.put("canRead", imageFile.canRead());
        info.put("lastModified", imageFile.exists() ? new java.util.Date(imageFile.lastModified()).toString() : null);

        String[] possibleUrls = new String[] {
                "/static/upload/" + filename,
                "/mental/static/upload/" + filename,
                "/upload/" + filename,
                "/mental/upload/" + filename
        };
        info.put("possibleUrls", possibleUrls);
        info.put("uploadDirSetting", uploadDir);
        info.put("uploadDirExists", uploadDirectory.exists());

        return Result.success(info);
    }

    @GetMapping("/direct-image/{filename:.+}")
    public ResponseEntity<Resource> getImageDirect(@PathVariable String filename) throws IOException {
        File uploadDirectory = new File(uploadDir);
        Path imagePath = Paths.get(uploadDirectory.getAbsolutePath(), filename);

        if (!Files.exists(imagePath)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(imagePath.toFile());
        String contentType = Files.probeContentType(imagePath);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + imagePath.getFileName().toString() + "\"")
                .body(resource);
    }

    @GetMapping("/list-uploads")
    public Result<?> listUploadDirectory() {
        File uploadDirectory = new File(uploadDir);
        if (!uploadDirectory.exists() || !uploadDirectory.isDirectory()) {
            return Result.error("上传目录不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("directory", uploadDirectory.getAbsolutePath());
        result.put("exists", uploadDirectory.exists());

        if (uploadDirectory.exists()) {
            File[] files = uploadDirectory.listFiles();
            String[] fileNames = new String[files != null ? files.length : 0];
            Map<String, Long> fileSizes = new HashMap<>();

            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    fileNames[i] = files[i].getName();
                    fileSizes.put(files[i].getName(), files[i].length());
                }
            }

            result.put("files", fileNames);
            result.put("fileSizes", fileSizes);
            result.put("fileCount", fileNames.length);
        }

        return Result.success(result);
    }

    @PostMapping("/upload")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Result.error("上传文件为空");
            }
            String originalFilename = file.getOriginalFilename();
            String fileName = java.util.UUID.randomUUID() + "_" + originalFilename;
            java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir, fileName);
            java.nio.file.Files.createDirectories(uploadPath.getParent());
            file.transferTo(uploadPath.toFile());
            String url = "/mental/static/upload/" + fileName;
            return Result.success(url);
        } catch (Exception e) {
            logger.error("图片上传失败", e);
            return Result.error("图片上传失败: " + e.getMessage());
        }
    }

    /**
     * 处理带有特殊标记（如:1）的图片文件名
     * 例如: 7c96122e-1889-4979-b...-f1ee1c873693.jpg:1
     */
    @GetMapping("/image/{filename:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) throws IOException {
        logger.info("请求图片: {}", filename);

        // 处理可能包含:1这样后缀的文件名
        String cleanFilename = filename;
        if (filename.contains(":")) {
            cleanFilename = filename.split(":")[0];
            logger.info("处理后的文件名: {}", cleanFilename);
        }

        File uploadDirectory = new File(uploadDir);

        // 首先在主上传目录查找
        Path imagePath = Paths.get(uploadDirectory.getAbsolutePath(), cleanFilename);
        logger.info("查找图片路径: {}", imagePath);

        // 如果在主目录找不到，尝试在image子目录查找
        if (!Files.exists(imagePath)) {
            logger.warn("图片不存在: {}", imagePath);
            Path imageSubdirPath = Paths.get(uploadDirectory.getAbsolutePath(), "image", cleanFilename);
            logger.info("尝试在image子目录查找: {}", imageSubdirPath);

            if (Files.exists(imageSubdirPath)) {
                logger.info("在image子目录找到图片: {}", imageSubdirPath);
                imagePath = imageSubdirPath;
            } else {
                // 尝试在其他可能的位置查找
                String[] possiblePaths = {
                        "src/main/resources/static/upload/image/",
                        "src/main/resources/static/upload/",
                        "static/upload/image/",
                        "static/upload/",
                        "upload/image/",
                        "upload/"
                };

                for (String path : possiblePaths) {
                    Path alternatePath = Paths.get(path, cleanFilename);
                    logger.info("尝试替代路径: {}", alternatePath);
                    if (Files.exists(alternatePath)) {
                        logger.info("在替代路径找到图片: {}", alternatePath);
                        imagePath = alternatePath;
                        break;
                    }
                }

                // 如果仍然找不到，返回404
                if (!Files.exists(imagePath)) {
                    logger.error("无法找到图片: {}", cleanFilename);
                    return ResponseEntity.notFound().build();
                }
            }
        }

        Resource resource = new FileSystemResource(imagePath.toFile());
        String contentType = Files.probeContentType(imagePath);
        if (contentType == null) {
            contentType = "image/jpeg"; // 默认为JPEG
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + cleanFilename + "\"")
                .body(resource);
    }
}
