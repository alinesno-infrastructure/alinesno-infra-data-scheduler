import request from '@/utils/request'
import { parseStrEmpty } from "@/utils/ruoyi";

var prefix = '/api/infra/data/scheduler/nodeLogs/' ;
var managerUrl = {
    readLog : prefix + "readLog" ,
}

// processInstanceId: string, start: number (optional)
export function readLog(processInstanceId, start = 0 , nodeId){
  const params = `?processInstanceId=${parseStrEmpty(processInstanceId)}&start=${start}&nodeId=${parseStrEmpty(nodeId)}`;
  return request({
    url: managerUrl.readLog + params,
    method: 'get'
  })
}