import request from '@/utils/request'

// 接口配置项
var prefix = '/api/infra/data/scheduler/dashboard/' ;
var managerUrl = {
   recentlyProcess: prefix + 'recentlyProcess'
}

// 最近执行任务
export function recentlyProcess() {
  return request({
    url: managerUrl.recentlyProcess,
    method: 'get'
  })
}
