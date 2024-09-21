# alinesno-infra-data-scheduler

定时任务编排服务，使用多任务实例和编排可视化管理

## 性能要求
 
性能指标：

- 定时任务支持不少于2000个
- 任务时长最大不少于12小时

## 表设计

系统表

- project 项目表
- datasource 数据源表
- environment 运行环境表
- udfs 用户自定义函数表
- resources 资源表
- alert 告警配置表

流程表设计

- process_definition 流程定义表
- process_instance 流程实例表
- process_definition_task_relation 流程定义任务关联表
- task_definition 任务定义表
- task_instance 任务实例表

## 鸣谢

- 集成学习参考资料[Quartz应用与集群原理分析](https://tech.meituan.com/2014/08/31/mt-crm-quartz.html)
- 集成定时任务框架[quartz](https://github.com/kagkarlsson/db-scheduler)
- 任务参数参考[任务总体存储结构](https://docs.devlive.org/read/apache-dolphin-scheduler-zh-3.2.1/Architecture-Task-Structure)