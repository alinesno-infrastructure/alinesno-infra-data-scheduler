import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

public class ImageCompressor {
    // 图片大小阈值，超过此值将被压缩（单位：字节），这里设置为100KB
    private static final long SIZE_THRESHOLD = 1024 * 100;

    // 压缩质量，0.0f-1.0f，值越小压缩率越高
    private static final float COMPRESSION_QUALITY = 0.7f;

    // 最大宽度限制，超过此宽度将被压缩
    private static final int MAX_WIDTH = 400;

    // 最大高度限制，超过此高度将被压缩
    private static final int MAX_HEIGHT = 800;

    // 要处理的图片目录（直接在这里指定目录路径）
    private static final String IMAGE_DIRECTORY = "public\\images\\team"; // 替换为你的图片目录

    public static void main(String[] args) {
        File directory = new File(IMAGE_DIRECTORY);

        // 检查目录是否存在
        if (!directory.exists() || !directory.isDirectory()) {
            System.out.println("指定的路径不存在或不是一个目录: " + IMAGE_DIRECTORY);
            return;
        }

        System.out.println("开始处理目录: " + IMAGE_DIRECTORY);
        System.out.println("最大宽度限制: " + MAX_WIDTH + "px");
        System.out.println("最大高度限制: " + MAX_HEIGHT + "px");
        System.out.println("文件大小阈值: " + SIZE_THRESHOLD / 1024 + "KB");

        // 压缩目录中的所有图片
        compressImagesInDirectory(directory);
        System.out.println("图片压缩完成");
    }

    /**
     * 压缩目录中的所有图片
     */
    private static void compressImagesInDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                // 递归处理子目录
                compressImagesInDirectory(file);
            } else if (isImageFile(file)) {
                // 处理图片文件
                try {
                    compressImageIfNeeded(file);
                } catch (IOException e) {
                    System.out.println("压缩图片失败: " + file.getAbsolutePath() + ", 错误: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 检查文件是否为图片
     */
    private static boolean isImageFile(File file) {
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") ||
                fileName.endsWith(".png") || fileName.endsWith(".gif") ||
                fileName.endsWith(".bmp");
    }

    /**
     * 如果图片大小超过阈值或尺寸超过最大限制则进行压缩
     */
    private static void compressImageIfNeeded(File imageFile) throws IOException {
        long originalSize = imageFile.length();
        BufferedImage originalImage = ImageIO.read(imageFile);
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // 判断是否需要压缩：大小超过阈值，或者宽度/高度超过最大限制
        boolean needSizeCompression = originalSize > SIZE_THRESHOLD;
        boolean needDimensionCompression = originalWidth > MAX_WIDTH || originalHeight > MAX_HEIGHT;

        if (!needSizeCompression && !needDimensionCompression) {
            // 既不超过大小阈值，也不超过尺寸限制，无需压缩
            return;
        }

        System.out.println("正在处理: " + imageFile.getAbsolutePath() +
                " (" + originalSize / 1024 + "KB, " +
                originalWidth + "x" + originalHeight + ")");

        // 计算压缩比例：确保压缩后宽度不超过MAX_WIDTH，高度不超过MAX_HEIGHT，同时保持原比例
        double scaleRatio = 1.0;
        if (originalWidth > MAX_WIDTH) {
            scaleRatio = Math.min(scaleRatio, (double) MAX_WIDTH / originalWidth);
        }
        if (originalHeight > MAX_HEIGHT) {
            scaleRatio = Math.min(scaleRatio, (double) MAX_HEIGHT / originalHeight);
        }

        // 计算压缩后的尺寸
        int newWidth = (int) (originalWidth * scaleRatio);
        int newHeight = (int) (originalHeight * scaleRatio);

        System.out.println("尺寸压缩: " + originalWidth + "x" + originalHeight +
                " -> " + newWidth + "x" + newHeight +
                " (比例: " + String.format("%.2f", scaleRatio) + ")");

        // 创建压缩后的图片
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight,
                originalImage.getType() == 0 ? BufferedImage.TYPE_INT_RGB : originalImage.getType());
        Graphics2D g = resizedImage.createGraphics();
        // 保持图片质量
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g.dispose();

        // 获取文件格式
        String formatName = getFileFormat(imageFile);

        // 保存压缩后的图片，覆盖原文件
        try (OutputStream os = new FileOutputStream(imageFile);
             ImageOutputStream ios = ImageIO.createImageOutputStream(os)) {

            // 获取合适的ImageWriter
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(formatName);
            if (!writers.hasNext()) {
                throw new IOException("找不到合适的图片写入器 for format: " + formatName);
            }

            ImageWriter writer = writers.next();
            ImageWriteParam param = writer.getDefaultWriteParam();

            // 设置压缩参数（如果文件大小超标）
            if (param.canWriteCompressed() && needSizeCompression) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(COMPRESSION_QUALITY);
            }

            writer.setOutput(ios);
            writer.write(null, new IIOImage(resizedImage, null, null), param);
            writer.dispose();
        }

        long newSize = imageFile.length();
        System.out.println("处理完成: " + imageFile.getAbsolutePath() +
                " (" + newSize / 1024 + "KB, 减少了 " +
                (100 - (newSize * 100 / originalSize)) + "%)");
    }

    /**
     * 获取文件格式（如jpg, png等）
     */
    private static String getFileFormat(File file) {
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return "jpg"; // 默认格式
    }
}
    