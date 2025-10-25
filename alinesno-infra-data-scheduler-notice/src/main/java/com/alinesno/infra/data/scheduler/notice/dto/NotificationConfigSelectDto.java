package com.alinesno.infra.data.scheduler.notice.dto;

import com.alinesno.infra.data.scheduler.entity.NotificationConfigEntity;
import lombok.Data;

import java.util.List;

/**
 * 通知配置选择DTO
 */
@Data
public class NotificationConfigSelectDto {

    private Long id ;

    private String name ;

    private String provider;

    /**
     * 转换
     * @param list
     * @return
     */
    public static List<NotificationConfigSelectDto> convert(List<NotificationConfigEntity> list) {

        return list.stream().map(entity -> {
            NotificationConfigSelectDto dto = new NotificationConfigSelectDto();
            dto.setId(entity.getId());
            dto.setName(entity.getName());
            dto.setProvider(entity.getProvider());
            return dto;
        }).toList();

    }
}
