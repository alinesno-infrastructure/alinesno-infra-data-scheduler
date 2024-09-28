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

	private String readerName;

	@NotBlank(message = "连接不能为空")
	private String readerUrl;

	private String readerUsername;

	private String readerPasswd;

	private String accessKey ;

	private String secretKey ;

	private String readerPort;

	@NotBlank(message = "类型不能为空")
	private String readerType;

	private String owner;

	@NotBlank(message = "操作类型不能为空")
	private String operationType;

}