package com.alinesno.infra.data.scheduler.service.impl;

import com.alinesno.infra.common.core.service.impl.IBaseServiceImpl;
import com.alinesno.infra.data.scheduler.entity.SecretsEntity;
import com.alinesno.infra.data.scheduler.mapper.SecretsMapper;
import com.alinesno.infra.data.scheduler.service.ISecretsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 【请填写功能名称】Service业务层处理
 *
 * @version 1.0.0
 * @autor luoxiaodong
 */
@Slf4j
@Service
public class SecretsServiceImpl extends IBaseServiceImpl<SecretsEntity, SecretsMapper> implements ISecretsService {

    @Override
public Map<String, String> secretMap() {
    try {
        List<SecretsEntity> list = this.list();

        // 确保列表不为空
        if (list == null || list.isEmpty()) {
            return Collections.emptyMap();
        }

        // 确保每个实体对象不为null
        return list.stream()
                .filter(Objects::nonNull) // 过滤掉null对象
                .collect(Collectors.toMap(
                        SecretsEntity::getSecName,
                        SecretsEntity::getSecValue,
                        (oldValue, newValue) -> oldValue, // 如果有重复键，保留旧值
                        HashMap::new // 使用HashMap作为收集器工厂
                ));
    } catch (Exception e) {
        // 异常处理
        log.error("Error occurred while fetching secret map: " + e.getMessage());
        return Collections.emptyMap();
    }
}

    @Override
    public Map<String, String> getSecretMapByOrgId(Long orgId) {

        if(orgId == null){
            return Collections.emptyMap();
        }

        try {
            LambdaQueryWrapper<SecretsEntity> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SecretsEntity::getOrgId, orgId);

            List<SecretsEntity> list = this.list(queryWrapper);

            // 确保列表不为空
            if (list == null || list.isEmpty()) {
                return Collections.emptyMap();
            }

            // 确保每个实体对象不为null
            return list.stream()
                    .filter(Objects::nonNull) // 过滤掉null对象
                    .collect(Collectors.toMap(
                            SecretsEntity::getSecName,
                            SecretsEntity::getSecValue,
                            (oldValue, newValue) -> oldValue, // 如果有重复键，保留旧值
                            HashMap::new // 使用HashMap作为收集器工厂
                    ));
        } catch (Exception e) {
            // 异常处理
            log.error("Error occurred while fetching org secret map: " + e.getMessage());
            return Collections.emptyMap();
        }
    }

}
