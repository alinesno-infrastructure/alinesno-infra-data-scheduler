import request from '@/utils/request'

// 接口配置项
var prefix = '/api/infra/data/scheduler/dashboard/' ;
var managerUrl = {
   recentlyProcess: prefix + 'recentlyProcess',
   simpleStatistics: prefix + 'simpleStatistics'
}

// 简单统计
export function simpleStatistics() {
  return request({
    url: managerUrl.simpleStatistics,
    method: 'get'
  })
}

// 最近执行任务
export function recentlyProcess() {
  return request({
    url: managerUrl.recentlyProcess,
    method: 'get'
  })
}
