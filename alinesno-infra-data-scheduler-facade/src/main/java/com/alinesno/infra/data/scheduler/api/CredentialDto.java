package com.alinesno.infra.data.scheduler.api;

import com.alinesno.infra.common.facade.base.BaseDto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * CredentialEntity凭据实体信息
 */
@Data
public class CredentialDto extends BaseDto {

    @NotBlank(message = "标识不能为空")
    private String credentialId;

    private long projectId ; // 所属项目

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password; // 注意：在实际应用中应考虑对密码进行加密存储

    private String type;

    @NotBlank(message = "描述不能为空")
    private String description;

    @NotBlank(message = "范围不能为空")
    private String credentialScope ;

}