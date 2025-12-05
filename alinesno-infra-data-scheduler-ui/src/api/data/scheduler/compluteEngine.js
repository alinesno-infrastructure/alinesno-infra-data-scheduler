// src/api/computeEngineApi.js
import request from '@/utils/request'

const prefix = '/api/infra/data/scheduler/computeEngine'

export function getConfig() {
  return request({
    url: `${prefix}/getConfig`,
    method: 'get'
  })
}

export function saveConfig(payload) {
  return request({
    url: `${prefix}/saveConfig`,
    method: 'post',
    data: payload
  })
}

export function probeEngineOnServer(engineAddress, apiToken) {
  return request({
    url: `${prefix}/probeHealth`,
    method: 'get',
    params: { engineAddress, apiToken}
  })
}