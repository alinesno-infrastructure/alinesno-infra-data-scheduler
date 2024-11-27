package com.alinesno.infra.data.scheduler.executor.handle;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSONObject;
import com.alinesno.infra.data.scheduler.api.ParamsDto;
import com.alinesno.infra.data.scheduler.executor.AbstractExecutorService;
import com.alinesno.infra.data.scheduler.executor.bean.TaskInfoBean;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Map;

@Slf4j
@Service("ansibleExecutor")
public class AnsibleExecutor extends AbstractExecutorService {

    @SneakyThrows
    @Override
    public void execute(TaskInfoBean taskInfo) {
        configTaskParams(taskInfo, this);
        log.debug("AnsibleExecutor execute");

        ParamsDto params = getParamsDto() ;
        String inventory = getResources().get(0) ;
        String extraVars = params.getExtraVars() ;

        String rawScript = readerRawScript() ;

        log.debug("rawScript: {}", rawScript) ;

        // 将python脚本写到临时文件
        File ansiblePlaybookFile = new File(getWorkspace(), "ansible_" + IdUtil.getSnowflakeNextIdStr() + ".yaml") ;
        FileUtils.writeStringToFile(ansiblePlaybookFile, rawScript  , Charset.defaultCharset() , false);

        // 使用 StringBuilder 构建命令
        String commandBuilder = "ansible-playbook " +
                " -i " +
                inventory + " " +
                ansiblePlaybookFile.getAbsoluteFile() ;

        if(StringUtils.isNotBlank(extraVars)){
            Map<String, Object> map = bodyToMap(extraVars) ;
            commandBuilder += " -e " + JSONObject.toJSONString(map);
        }

        log.debug("commandBuilder:{}" , commandBuilder);

        runCommand(commandBuilder) ;

    }

}
