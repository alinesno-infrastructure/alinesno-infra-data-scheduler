package com.alinesno.infra.data.scheduler.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum ResourceTypeEnums {

    FILE(0, "file"),
    UDF(1, "udf");

    ResourceTypeEnums(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @EnumValue
    private final int code;
    private final String desc;

}
