package com.alinesno.infra.data.scheduler.workflow.generic;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.db.Entity;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.util.ListUtils;
import com.alibaba.fastjson.JSON;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipInputStream;

import static com.baomidou.mybatisplus.core.toolkit.StringUtils.camelToUnderline;

/**
 * 通用Excel处理父类
 *
 * @author luodong
 * @version 1.0.0
 */
@Slf4j
public abstract class GenericExcelParent {

    // 写一个方法通过文件名获取日期，格式是_yyyy_mm_dd
    protected static String getDateFromFileName(String fileName) {
        // 文件名格式为：xxxxxx_yyyy_mm_dd.xlsx
        return fileName.substring(fileName.lastIndexOf("_") + 1, fileName.lastIndexOf("."));
    }

    // 创建一个postgres的Datasource
    protected static HikariDataSource createPostgresDatasource(
            String driverClass,
            String jdbcUrl,
            String username,
            String password
    ) {

        HikariConfig config = new HikariConfig();

        config.setDriverClassName(driverClass) ;
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);

        return new HikariDataSource(config);
    }

    /**
     * 从OSS下载ZIP文件
     */
    protected static Path downloadOssZip(String baseUrl, String dateParam, String token) throws Exception {
        // 标准化日期（如果为null则默认使用当天）
        String date = dateParam != null ? dateParam : LocalDate.now().format(DateTimeFormatter.ISO_DATE);

        // 构建带日期参数的URL（复用参考代码中的助手方法）
        String url = buildUrlWithDate(baseUrl, date);

        // 通过HTTP GET和token头下载文件
        Path tempZip = Files.createTempFile("student-scores-", ".zip");
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Download-Token", token)
                .GET()
                .build();

        HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(tempZip));
        if (response.statusCode() != 200) {
            throw new IOException("下载ZIP文件失败: HTTP " + response.statusCode());
        }
        return tempZip;
    }

    /**
     * 构建带日期参数的URL
     */
    protected static String buildUrlWithDate(String baseUrl, String date) {
        // 在URL中添加/替换"date"参数（从参考代码复用）
        URL url;
        try {
            url = new URL(baseUrl);
            LinkedHashMap<String, String> params = getParams(date, url);

            // 构建新的URL
            StringBuilder newQuery = new StringBuilder();
            for (java.util.Map.Entry<String, String> entry : params.entrySet()) {
                if (!newQuery.isEmpty()) {
                    newQuery.append("&");
                }
                newQuery.append(entry.getKey()).append("=").append(entry.getValue());
            }

            return new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getPath() + "?" + newQuery.toString()).toString();
        } catch (Exception e) {
            throw new RuntimeException("构建URL失败", e);
        }
    }

    @NotNull
    private static LinkedHashMap<String, String> getParams(String date, URL url) {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();

        // 解析现有查询参数
        if (url.getQuery() != null) {
            String[] queryPairs = url.getQuery().split("&");
            for (String pair : queryPairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    params.put(keyValue[0], keyValue[1]);
                }
            }
        }

        // 添加/更新日期参数
        params.put("date", date);
        return params;
    }

    /**
     * 解压ZIP文件到临时目录
     */
    protected static Path unzipToTemp(Path zipPath) throws Exception {
        Path unzipDir = Files.createTempDirectory("student-scores-unzipped-");
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = unzipDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.copy(zis, entryPath);
                }
                zis.closeEntry();
            }
        }
        return unzipDir;
    }

    /**
     * 读取Excel文件
     *
     * @param fileName Excel文件路径
     * @param clazz 数据模型类
     * @param dataConsumer 数据处理消费者
     * @param <T> 数据类型
     */
    protected static <T> void read(String fileName, Class<T> clazz, Consumer<java.util.List<T>> dataConsumer) {
        GenericExcelDataListener<T> listener = new GenericExcelDataListener<>(dataConsumer);
        EasyExcel.read(fileName, clazz, listener).sheet().doRead();
    }

    /**
     * 读取Excel文件（使用默认的数据处理逻辑）
     *
     * @param fileName Excel文件路径
     * @param clazz 数据模型类
     * @param <T> 数据类型
     */
    protected static <T> void read(String fileName, Class<T> clazz) {
        GenericExcelDataListener<T> listener = new GenericExcelDataListener<>();
        EasyExcel.read(fileName, clazz, listener).sheet().doRead();
    }

    /**
     * 读取指定sheet的Excel文件
     *
     * @param fileName Excel文件路径
     * @param clazz 数据模型类
     * @param dataConsumer 数据处理消费者
     * @param sheetNo sheet编号（从0开始）
     * @param <T> 数据类型
     */
    protected static <T> void read(String fileName, Class<T> clazz, Consumer<java.util.List<T>> dataConsumer, int sheetNo) {
        GenericExcelDataListener<T> listener = new GenericExcelDataListener<>(dataConsumer);
        EasyExcel.read(fileName, clazz, listener).sheet(sheetNo).doRead();
    }

    /**
     * 读取指定sheet名称的Excel文件
     *
     * @param fileName Excel文件路径
     * @param clazz 数据模型类
     * @param dataConsumer 数据处理消费者
     * @param sheetName sheet名称
     * @param <T> 数据类型
     */
    protected static <T> void read(String fileName, Class<T> clazz, Consumer<java.util.List<T>> dataConsumer, String sheetName) {
        GenericExcelDataListener<T> listener = new GenericExcelDataListener<>(dataConsumer);
        EasyExcel.read(fileName, clazz, listener).sheet(sheetName).doRead();
    }


    protected static class GenericExcelDataListener<T> extends AnalysisEventListener<T> {
        /**
         * 每隔5条存储数据库，实际使用中可以100条，然后清理list ，方便内存回收
         */
        private static final int BATCH_COUNT = 500;
        private List<T> cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);

        /**
         * 数据保存处理器
         */
        private final Consumer<List<T>> dataConsumer;

        /**
         * 构造函数
         * @param dataConsumer 数据处理消费者，用于处理批量数据
         */
        public GenericExcelDataListener(Consumer<List<T>> dataConsumer) {
            this.dataConsumer = dataConsumer;
        }

        /**
         * 无参构造函数，使用默认的数据保存逻辑
         */
        public GenericExcelDataListener() {
            this.dataConsumer = this::defaultSaveData;
        }

        @Override
        public void invoke(T data, AnalysisContext context) {
            log.info("解析到一条数据:{}", JSON.toJSONString(data));
            cachedDataList.add(data);
            if (cachedDataList.size() >= BATCH_COUNT) {
                saveData();
                cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
            }
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            saveData();
            log.info("所有数据解析完成！");
        }

        /**
         * 保存数据
         */
        private void saveData() {
            if (dataConsumer != null) {
                dataConsumer.accept(cachedDataList);
            }
        }

        /**
         * 默认的数据保存逻辑
         */
        private void defaultSaveData(List<T> dataList) {
            log.info("{}条数据，开始存储数据库！", dataList.size());
            // 这里可以添加默认的保存逻辑，比如打印日志等
            log.info("存储数据库成功！");
        }
    }

    protected static final List<String> ignoreFields = List
            .of("$static_class_info" , "__$st_m_c" , "meta_class");

    /**
     * 将对象转换为Entity，字段名转换为下划线格式
     *
     * @param obj 对象
     * @param tableName 表名
     * @return Entity对象
     */
    protected static Entity convertToEntity(Object obj, String tableName) {
        if (obj == null) {
            return null;
        }

        Entity entity = Entity.create(tableName);
        Field[] fields = ReflectUtil.getFields(obj.getClass());

        for (Field field : fields) {
            try {
                field.setAccessible(true);
                Object value = field.get(obj);
                if (value != null) {
                    // 将驼峰字段名转换为下划线格式
                    String fieldName = camelToUnderline(field.getName());
                    if(ignoreFields.contains(fieldName)){
                        continue;
                    }
                    entity.set(fieldName, value);
                }
            } catch (IllegalAccessException e) {
                log.warn("获取字段值失败: {}", field.getName(), e);
            }
        }

        return entity;
    }

}
