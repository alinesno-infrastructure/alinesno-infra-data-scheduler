package com.alinesno.infra.data.scheduler.api;


import com.alinesno.infra.common.facade.base.BaseDto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author luoxiaodong
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DataSourceDto extends BaseDto {

	@NotBlank(message = "描述不能为空")
	private String readerDesc;

//	@NotBlank(message = "名称不能为空")
	private String readerName;

	@NotBlank(message = "连接不能为空")
	private String readerUrl;

//	@ConditionalNotBlank(conditionField = "readerType", conditionValue = "mysql|oracle", message = "用户名不能为空")
	private String readerUsername;

//	@ConditionalNotBlank(conditionField = "readerType", conditionValue = "mysql|oracle", message = "密码不能为空")
	private String readerPasswd;

//	@NotBlank(message = "accessKey不能为空")
	private String accessKey ;

//	@NotBlank(message = "secretKey不能为空")
	private String secretKey ;

//	@NotBlank(message = "端口不能为空")
	private String readerPort;

	@NotBlank(message = "类型不能为空")
	private String readerType;

	@NotBlank(message = "数据来源组织不能为空")
	private String owner;

	@NotBlank(message = "操作类型不能为空")
	private String operationType;

}