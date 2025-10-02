import request from '@/utils/request'
import { parseStrEmpty } from "@/utils/ruoyi";

/**
 * 通知配置接口
 *
 * author: luoxiaodong
 * since : 1.0.0
 */

// 接口前缀
var prefix = '/api/infra/data/scheduler/notificationConfig/' ;

// 接口管理对象，便于维护
var managerUrl = {
  datatables: prefix + "datatables",
  get: prefix + "getConfig/",
  save: prefix + "saveConfig",
  remove: prefix + "removeConfig/",
  list: prefix + "list",
  listAllIM: prefix + "listAllIM"
}

/**
 * 列出所有的IM工具列表
 */
export function listAllIM() {
  return request({
    url: managerUrl.listAllIM,
    method: 'get'
  })
}

/**
 * datatables 查询（保持和原来一致，使用 params）
 * @param query datatables 请求参数对象
 */
export function datatables(query) {
  return request({
    url: managerUrl.datatables,
    method: 'post',
    params: query
  })
}

/**
 * 获取单条配置
 * @param id 配置 id
 */
export function getCredential(id) {
  return request({
    url: managerUrl.get + parseStrEmpty(id),
    method: 'get'
  })
}

/**
 * 保存或更新配置（POST body）
 * @param entity 配置对象
 */
export function saveCredential(entity) {
  return request({
    url: managerUrl.save,
    method: 'post',
    data: entity
  })
}

/**
 * 删除配置
 * @param id 配置 id
 */
export function removeCredential(id) {
  return request({
    url: managerUrl.remove + parseStrEmpty(id),
    method: 'delete'
  })
}

/**
 * 列表（用于下拉或前端过滤，保持使用 params 以与原风格一致）
 * @param provider 可选，按 provider 过滤
 */
export function listCredential(provider) {
  return request({
    url: managerUrl.list,
    method: 'post',
    params: provider ? { provider } : {}
  })
}