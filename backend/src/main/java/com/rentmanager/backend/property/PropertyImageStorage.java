package com.rentmanager.backend.property;

import com.rentmanager.backend.error.ApiException;
import com.rentmanager.backend.error.ErrorCode;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** Stores property images on the local filesystem (see docs/ARCHITECTURE.md; cloud storage is a Phase 17 item). */
@Component
public class PropertyImageStorage {

  private static final long MAX_BYTES = 5 * 1024 * 1024;
  private static final Map<String, String> EXTENSIONS = Map.of(
      "image/jpeg", "jpg",
      "image/png", "png",
      "image/webp", "webp");

  private final Path directory;

  public PropertyImageStorage(@Value("${app.uploads.dir:./uploads}") String uploadsDir) {
    this.directory = Paths.get(uploadsDir, "properties").toAbsolutePath().normalize();
  }

  @PostConstruct
  void createDirectory() throws IOException {
    Files.createDirectories(directory);
  }

  public StoredImage store(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new ApiException(ErrorCode.UNSUPPORTED_IMAGE_TYPE);
    }
    if (file.getSize() > MAX_BYTES) {
      throw new ApiException(ErrorCode.FILE_TOO_LARGE);
    }
    String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
    String extension = EXTENSIONS.get(contentType);
    if (extension == null || !hasValidSignature(file, extension)) {
      throw new ApiException(ErrorCode.UNSUPPORTED_IMAGE_TYPE);
    }
    String fileName = UUID.randomUUID() + "." + extension;
    try {
      file.transferTo(directory.resolve(fileName));
    } catch (IOException | IllegalStateException exception) {
      throw new ApiException(ErrorCode.INTERNAL_ERROR);
    }
    return new StoredImage(fileName, contentType);
  }

  public void delete(String fileName) {
    try {
      Files.deleteIfExists(directory.resolve(fileName).normalize());
    } catch (IOException exception) {
      throw new ApiException(ErrorCode.INTERNAL_ERROR);
    }
  }

  /** Magic-byte check so a renamed file cannot be served as an image. */
  private static boolean hasValidSignature(MultipartFile file, String extension) {
    byte[] head;
    try (InputStream input = file.getInputStream()) {
      head = input.readNBytes(12);
    } catch (IOException exception) {
      return false;
    }
    return switch (extension) {
      case "jpg" -> head.length >= 3
          && head[0] == (byte) 0xFF && head[1] == (byte) 0xD8 && head[2] == (byte) 0xFF;
      case "png" -> head.length >= 8
          && head[0] == (byte) 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G';
      case "webp" -> head.length >= 12
          && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
          && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P';
      default -> false;
    };
  }

  public record StoredImage(String fileName, String contentType) {}
}
