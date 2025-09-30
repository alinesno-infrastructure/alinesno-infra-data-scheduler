package com.alinesno.infra.data.scheduler.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alinesno.infra.common.core.service.impl.IBaseServiceImpl;
import com.alinesno.infra.common.facade.datascope.PermissionQuery;
import com.alinesno.infra.data.scheduler.api.ProbeResultDto;
import com.alinesno.infra.data.scheduler.entity.ComputeEngineEntity;
import com.alinesno.infra.data.scheduler.mapper.ComputeEngineMapper;
import com.alinesno.infra.data.scheduler.service.IComputeEngineService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 分布式计算引擎配置 Service 实现
 *
 * @version 1.0.0
 * @author luoxiaodong
 */
@Slf4j
@Service
public class ComputeEngineServiceImpl extends IBaseServiceImpl<ComputeEngineEntity, ComputeEngineMapper> implements IComputeEngineService {

    @Override
    public ComputeEngineEntity getCurrentConfig(PermissionQuery  query) {
        LambdaQueryWrapper<ComputeEngineEntity> qw = new LambdaQueryWrapper<>();
        qw.eq(ComputeEngineEntity::getOrgId, query.getOrgId()) ;
        qw.orderByDesc(ComputeEngineEntity::getUpdateTime)
                .last("LIMIT 1");

        List<ComputeEngineEntity> es = list(qw) ;
        return es == null || es.isEmpty() ? defaultConfig() : es.get(0);
    }

    private ComputeEngineEntity defaultConfig() {
        ComputeEngineEntity cfg = new ComputeEngineEntity();
        cfg.setComputeEngine("spark");
        cfg.setEngineAddress("");
        cfg.setAdminUser("");
        cfg.setRequestTimeout(30);
        return cfg;
    }

    @Override
    public boolean saveOrUpdateConfig(ComputeEngineEntity entity) {

        // 先删除组织之前所有的配置，重新保存最新的
        LambdaUpdateWrapper<ComputeEngineEntity> qw = new LambdaUpdateWrapper<>();
        qw.eq(ComputeEngineEntity::getOrgId, entity.getOrgId()) ;
        this.remove(qw);

        // 保存
        return this.saveOrUpdate(entity);
    }

    @Override
    public ProbeResultDto probeEngineHealth(String engineAddress, String adminUser) {
        // 基本校验（使用Hutool的StrUtil简化空判断）
        if (StrUtil.isBlank(engineAddress)) {
            return new ProbeResultDto(false, "engineAddress 为空", 0);
        }
        String addr = engineAddress.trim();

        // 只允许 http/https
        if (!addr.startsWith("http://") && !addr.startsWith("https://")) {
            return new ProbeResultDto(false, "只支持 http/https 地址", 0);
        }

        // 构建健康检查 URL（使用Hutool的StrUtil处理URL）
        String base = StrUtil.removeSuffix(addr, "/"); // 更高效地去掉尾部斜杠
        String healthUrl = base + "/health" + "?adminUser=" + adminUser;

        try {
            // 使用Hutool的HttpUtil发送请求，自动处理参数和编码
            HttpResponse result = HttpUtil.createGet(healthUrl).execute();

            // 处理响应（Hutool 5.8.22中使用getStatus()获取状态码）
            String body = StrUtil.trimToEmpty(result.body());
            int statusCode = result.getStatus(); // 适配5.8.22版本的方法名

            if (result.isOk() && "SUCCESS".equalsIgnoreCase(body)) {
                return new ProbeResultDto(true, "健康检查通过", statusCode);
            } else {
                String msg = String.format("健康检查未通过, status=%d, body=%s", statusCode, body);
                return new ProbeResultDto(false, msg, statusCode);
            }
        } catch (Exception e) {
            log.error("健康检查请求异常", e);
            return new ProbeResultDto(false, "请求失败: " + e.getMessage(), 0);
        }
    }

    @Override
    public ComputeEngineEntity getCurrentConfig(Long orgId) {

        PermissionQuery query = new PermissionQuery();
        query.setOrgId(orgId);

        return this.getCurrentConfig(query) ;
    }

}