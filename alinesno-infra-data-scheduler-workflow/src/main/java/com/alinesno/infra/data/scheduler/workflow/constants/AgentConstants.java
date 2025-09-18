package com.alinesno.infra.data.scheduler.workflow.constants;

public interface AgentConstants {


    String STEP_START = "start" ; // 节点开始
    String STEP_PROCESS = "process" ; // 节点任务进行中
    String STEP_FINISH = "finish" ;  //  节点结束
    String STEP_ERROR = "error" ;  // 节点错误
    String EMPTY_RESULT = "" ;  //  空结果

    long ORG_AGENT_STORE_TYPE_ID = 9527L ;  // 角色类型ID
    long STORE_EMPTY_ORG_ID = 0L ;  // 商店组织为空

    /**
     * 操作成功
     */
    String SUCCESS = "SUCCESS" ;

    /**
     * 操作失败
     */
    String FAIL = "FAIL" ;

}