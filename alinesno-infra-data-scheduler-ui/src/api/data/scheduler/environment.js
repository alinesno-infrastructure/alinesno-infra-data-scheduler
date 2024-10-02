import request from '@/utils/request'
import { parseStrEmpty } from "@/utils/ruoyi";

/**
 * 环境接口操作
 * 
 * @author luoxiaodong
 * @since 1.0.0
 */

// 接口配置项
var prefix = '/api/infra/data/scheduler/environment/' ;
var managerUrl = {
    datatables : prefix +"datatables" ,
    createUrl: prefix + 'add' ,
    saveUrl: prefix + 'saveEnv' ,
    updateUrl: prefix +"updateEnv" ,
    statusUrl: prefix +"changeStatus" ,
    cleanUrl: prefix + "clean",
    detailUrl: prefix +"getEnv",
    removeUrl: prefix + "delete" ,
    getAllEnv: prefix + "getAllEnv" ,
    exportUrl: prefix + "exportExcel",
    changeField: prefix + "changeField",
    getAllEnvironment: prefix + "getAllEnvironment",
    downloadfile: prefix + "downloadfile"
}

// 修改字段
export function changStatusField(data){
  return request({
    url: managerUrl.changeField ,
    method: 'post',
    data: data
  })
}

// 查询所有的资源列表
export function getAllEnvironment() {
  return request({
    url: managerUrl.getAllEnvironment, 
    method: 'get'
  })
}

// 查询环境列表
export function listEnvironment(query) {
  return request({
    url: managerUrl.datatables ,
    method: 'post',
    params: query
  })
}

// 查询环境详细
export function getEnvironment(databaseId) {
  return request({
    url: managerUrl.detailUrl + '?id=' + parseStrEmpty(databaseId),
    method: 'get'
  })
}

// 新增环境
export function addEnvironment(data) {
  return request({
    url: managerUrl.saveUrl ,
    method: 'post',
    data: data
  })
}

// 修改环境
export function updateEnvironment(data) {
  return request({
    url: managerUrl.updateUrl ,
    method: 'put',
    data: data
  })
}

// 删除环境
export function delEnvironment(databaseId) {
  return request({
    url: managerUrl.removeUrl + '/' + parseStrEmpty(databaseId),
    method: 'delete'
  })
}

export function downloadFile(databaseId) {
    return request({
    })
}