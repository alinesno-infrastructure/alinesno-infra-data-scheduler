package com.alinesno.infra.data.scheduler.llm.service;

import com.alinesno.infra.common.facade.datascope.PermissionQuery;
import com.alinesno.infra.common.facade.services.IBaseService;
import com.alinesno.infra.data.scheduler.llm.dto.TestLlmModelDto;
import com.alinesno.infra.data.scheduler.llm.entity.LlmModelEntity;

import java.util.List;
import java.util.Map;

/**
 * 应用构建Service接口
 * 
 * @version 1.0.0
 * @author luoxiaodong
 */
public interface ILlmModelService extends IBaseService<LlmModelEntity> {

    /**
     * 模型测试结果
     * @param dto
     */
    String testLlmModel(TestLlmModelDto dto);

    /**
     * 获取语音模型声音
     *
     * @param modelId
     * @return
     */
    Map<String, Object> getVoiceModelSpeech(String modelId);

//    /**
//     * 语音合成，并返回生成的文件
//     * @param dto
//     * @return
//     */
//    String speechSynthesisFile(IndustryRoleEntity role , ChatMessageDto dto);

//    /**
//     * 语音识别
//     * @param role
//     * @param data
//     * @return 识别的内容
//     */
//    String speechRecognitionFile(IndustryRoleEntity role, String data);

    /**
     * 获取模型列表，同时包含公共模型
     * @param query
     * @param modelType
     * @return
     */
    List<LlmModelEntity> listLlmMode(PermissionQuery query, String modelType);
}