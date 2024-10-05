import request from '@/utils/request'
import { parseStrEmpty } from "@/utils/ruoyi";

/**
 * 环境接口操作
 * 
 * @author luoxiaodong
 * @since 1.0.0
 */

// 接口配置项
var prefix = '/api/infra/data/scheduler/secrets/' ;
var managerUrl = {
    datatables : prefix +"datatables" ,
    createUrl: prefix + 'add' ,
    saveUrl: prefix + 'saveSec' ,
    updateUrl: prefix +"updateSec" ,
    statusUrl: prefix +"changeStatus" ,
    cleanUrl: prefix + "clean",
    detailUrl: prefix +"findSecById",
    removeUrl: prefix + "delete" ,
    exportUrl: prefix + "exportExcel",
    changeField: prefix + "changeField",
    getAllSecrets: prefix + "getAllSecrets",
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
export function getAllSecrets() {
  return request({
    url: managerUrl.getAllSecrets, 
    method: 'get'
  })
}

// 查询环境列表
export function listSecrets(query) {
  return request({
    url: managerUrl.datatables ,
    method: 'post',
    params: query
  })
}

// 查询环境详细
export function getSecrets(id) {
  return request({
    url: managerUrl.detailUrl + '?id=' + parseStrEmpty(id),
    method: 'get'
  })
}

// 新增环境
export function addSecrets(data) {
  return request({
    url: managerUrl.saveUrl ,
    method: 'post',
    data: data
  })
}

// 修改环境
export function updateSecrets(data) {
  return request({
    url: managerUrl.updateUrl ,
    method: 'put',
    data: data
  })
}

// 删除环境
export function delSecrets(id) {
  return request({
    url: managerUrl.removeUrl + '/' + parseStrEmpty(id),
    method: 'delete'
  })
}

export function downloadFile(id) {
    return request({
    })
}