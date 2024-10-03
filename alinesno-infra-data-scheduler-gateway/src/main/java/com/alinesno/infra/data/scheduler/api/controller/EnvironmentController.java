package com.alinesno.infra.data.scheduler.api.controller;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alinesno.infra.common.core.constants.SpringInstanceScope;
import com.alinesno.infra.common.facade.pageable.DatatablesPageBean;
import com.alinesno.infra.common.facade.pageable.TableDataInfo;
import com.alinesno.infra.common.facade.response.AjaxResult;
import com.alinesno.infra.common.web.adapter.rest.BaseController;
import com.alinesno.infra.data.scheduler.api.EnvironmentDto;
import com.alinesno.infra.data.scheduler.entity.EnvironmentEntity;
import com.alinesno.infra.data.scheduler.service.IEnvironmentService;
import io.swagger.annotations.Api;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 处理与TransEntity相关的请求的Controller。
 * 继承自BaseController类并实现ITransService接口。
 *
 * @author luoxiaodong
 * @version 1.0.0
 */
@Slf4j
@Api(tags = "Trans")
@RestController
@Scope(SpringInstanceScope.PROTOTYPE)
@RequestMapping("/api/infra/data/scheduler/environment")
public class EnvironmentController extends BaseController<EnvironmentEntity, IEnvironmentService> {

    @Autowired
    private IEnvironmentService service;

    /**
     * 获取TransEntity的DataTables数据。
     *
     * @param request HttpServletRequest对象。
     * @param model   Model对象。
     * @param page    DatatablesPageBean对象。
     * @return 包含DataTables数据的TableDataInfo对象。
     */
    @ResponseBody
    @PostMapping("/datatables")
    public TableDataInfo datatables(HttpServletRequest request, Model model, DatatablesPageBean page) {
        log.debug("page = {}", ToStringBuilder.reflectionToString(page));
        return this.toPage(model, this.getFeign(), page);
    }

    /**
     * 配置默认环境
     */
    @GetMapping("/defaultEnv")
    public AjaxResult defaultEnv(@RequestParam("id") Long id) {
        service.defaultEnv(id);
        return AjaxResult.success("success");
    }


    /**
     * 获取到所有的资源列表，并返回如下格式:
     * [{key:xxx,value:xxx}]
     */
    @GetMapping("/getAllEnvironment")
    public AjaxResult getAllEnvironment(){
        List<EnvironmentEntity> list = service.list() ;
        AjaxResult result = AjaxResult.success(list) ;

        long defaultId = list.stream().filter(EnvironmentEntity::isDefaultEnv).findFirst().map(EnvironmentEntity::getId).orElse(0L);
        result.put("systemEnvId" ,defaultId) ;

        return result ;
    }

    /**
     * 获取配置
     */
    @GetMapping("/getEnv")
    public AjaxResult getEnv(@RequestParam("id") Long id) {
        EnvironmentEntity entity = service.getById(id);

        String jsonConfig = entity.getConfig();
        try {
            // 明确指定解析为HashMap<String, String>类型
            Map<String, String> envVars = JSONObject.parseObject(jsonConfig, new TypeReference<HashMap<String, String>>() {
            });

            // 将MapN转换成 key=value形式
            // 处理空值和特殊字符
            String configTxt = envVars.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + (entry.getValue() == null ? "" : entry.getValue().replace("\n", "\\n").replace("=", "\\=")))
                    .collect(Collectors.joining("\n"));

            entity.setConfig(configTxt);
        } catch (JSONException e) {
            // 处理JSON解析异常
            System.err.println("Failed to parse JSON config: " + e.getMessage());
            entity.setConfig("");
        }


        return AjaxResult.success(entity);
    }

    /**
     * 更新环境配置
     */
    @PutMapping("/updateEnv")
    public AjaxResult updateEnv(@RequestBody @Validated EnvironmentDto dto) {
        processEnvironment(dto, new Date());
        return ok();
    }

    /**
     * 保存认证
     *
     * @return
     */
    @PostMapping("/saveEnv")
    public AjaxResult saveEnv(@RequestBody @Validated EnvironmentDto dto) {
        processEnvironment(dto, new Date());
        return ok();
    }

    // 增加一个方法参数用于注入 Date 对象
    public void processEnvironment(EnvironmentDto dto, Date updateTime) {
        EnvironmentEntity entity = new EnvironmentEntity();
        BeanUtils.copyProperties(dto, entity);

        entity.setUpdateTime(updateTime);

        // 创建一个空的HashMap
        Map<String, String> envVars = new HashMap<>();

        // 使用 StringTokenizer
        StringTokenizer tokenizer = new StringTokenizer(dto.getConfig(), "\n");
        while (tokenizer.hasMoreTokens()) {
            String line = tokenizer.nextToken();
            if (line.contains("=")) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    envVars.put(parts[0], parts[1]);
                } else {
                    log.warn("Invalid config line: {}", line);
                }
            }
        }

        entity.setConfig(JSONObject.toJSONString(envVars));
        try {
            service.saveOrUpdate(entity);
        } catch (Exception e) {
            log.error("Failed to save environment entity", e);
            throw new RuntimeException("Failed to save environment entity", e);
        }
    }


    @Override
    public IEnvironmentService getFeign() {
        return this.service;
    }
}
