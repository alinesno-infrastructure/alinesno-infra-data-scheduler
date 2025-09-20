import request from '@/utils/request'
import { parseStrEmpty } from "@/utils/ruoyi";

/**
 * 流程操作
 * 
 * @author luoxiaodong
 * @since 1.0.0
 */

// 接口配置项
var prefix = '/api/infra/data/scheduler/flow/' ;
var managerUrl = {
    processAndSave: prefix + "processAndSave",  // 流程保存和解析
    publishFlow: prefix + "publish",  // 发布工作流
    getLatestPublishedFlow: prefix + "latestPublished",  // 获取最新版本已发布流程
    getUnpublishedFlow: prefix + "unpublished",  // 获取未发布流程
    publishedFlow: prefix + "published", // 发布流程 
    getLatestFlow: prefix + "latest" ,  // 获取最新流程
    tryRun: prefix + "tryRun",  // 尝试运行
}

// 尝试运行
export function tryRun(processDefinitionId) {
    return request({
        url: managerUrl.tryRun + "?processDefinitionId=" + parseStrEmpty(processDefinitionId),
        method: 'get'
    })
}

// 发布流程
export function publishedFlow(flowId) {
    return request({
        url: managerUrl.publishedFlow + "?flowId=" + parseStrEmpty(flowId),
        method: 'get'
    })
}

// 流程保存和解析
export function processAndSave(data, processDefinitionId) {
    return request({
        url: managerUrl.processAndSave + "?processDefinitionId=" + parseStrEmpty(processDefinitionId),
        method: 'post',
        data: data
    })
}

// 发布工作流
export function publishFlow(processDefinitionId) {
    return request({
        url: managerUrl.publishFlow + "?processDefinitionId=" + parseStrEmpty(processDefinitionId),
        method: 'post'
    })
}

// 获取最新流程
export function getLatestFlow(processDefinitionId) {
    return request({
        url: managerUrl.getLatestFlow+ "?processDefinitionId=" + parseStrEmpty(processDefinitionId),
        method: 'get'
    })
}

// 获取最新版本的已发布流程
export function getLatestPublishedFlow(processDefinitionId) {
    return request({
        url: managerUrl.getLatestPublishedFlow + "?processDefinitionId=" + parseStrEmpty(processDefinitionId),
        method: 'get'
    })
}

// 获取指定角色的未发布流程
export function getUnpublishedFlow(processDefinitionId) {
    return request({
        url: managerUrl.getUnpublishedFlow + "?processDefinitionId=" + parseStrEmpty(processDefinitionId),
        method: 'get'
    })
}