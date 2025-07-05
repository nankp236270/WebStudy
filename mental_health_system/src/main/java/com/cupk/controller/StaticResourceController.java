package com.cupk.controller;

import com.cupk.service.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/static")
public class StaticResourceController {

    @Value("${upload.dir}")
    private String uploadDir;

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

        // 生成可能的访问路径
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

        // 清理文件名，去除可能的:1后缀
        String cleanFilename = filename;
        if (filename.contains(":")) {
            cleanFilename = filename.split(":")[0];
        }

        // 首先在主目录查找
        Path imagePath = Paths.get(uploadDirectory.getAbsolutePath(), cleanFilename);
        System.out.println("尝试查找图片: " + imagePath.toAbsolutePath());

        // 如果主目录不存在，尝试在image子目录
        if (!Files.exists(imagePath)) {
            Path imageSubdirPath = Paths.get(uploadDirectory.getAbsolutePath(), "image", cleanFilename);
            System.out.println("主目录未找到，尝试image子目录: " + imageSubdirPath.toAbsolutePath());

            if (Files.exists(imageSubdirPath)) {
                imagePath = imageSubdirPath;
                System.out.println("在image子目录找到图片: " + imagePath);
            } else {
                // 尝试在常见位置查找
                String[] possiblePaths = {
                        "src/main/resources/static/upload/image/",
                        "static/upload/image/",
                        "src/main/resources/static/upload/",
                        "../src/main/resources/static/upload/image/",
                        "../src/main/resources/static/upload/"
                };

                for (String path : possiblePaths) {
                    Path alternatePath = Paths.get(path, cleanFilename);
                    System.out.println("尝试备选路径: " + alternatePath.toAbsolutePath());

                    if (Files.exists(alternatePath)) {
                        imagePath = alternatePath;
                        System.out.println("在备选路径找到图片: " + imagePath);
                        break;
                    }
                }

                if (!Files.exists(imagePath)) {
                    System.out.println("所有路径都未找到图片: " + cleanFilename);
                    return ResponseEntity.notFound().build();
                }
            }
        } else {
            System.out.println("在主目录找到图片: " + imagePath);
        }

        Resource resource = new FileSystemResource(imagePath.toFile());
        String contentType = Files.probeContentType(imagePath);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        System.out.println("成功返回图片: " + imagePath + "，内容类型: " + contentType);
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
}