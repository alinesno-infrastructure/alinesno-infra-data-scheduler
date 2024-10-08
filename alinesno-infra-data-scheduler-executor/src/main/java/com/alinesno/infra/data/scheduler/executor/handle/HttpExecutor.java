package com.alinesno.infra.data.scheduler.executor.handle;

import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.alinesno.infra.data.scheduler.api.ParamsDto;
import com.alinesno.infra.data.scheduler.executor.AbstractExecutorService;
import com.alinesno.infra.data.scheduler.executor.bean.TaskInfoBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Http执行器
 */
@Slf4j
@Service("httpExecutor")
public class HttpExecutor extends AbstractExecutorService {

    @Override
    public void execute(TaskInfoBean taskInfo) {
        log.debug("HttpExecutor execute");

        ParamsDto params = getParamsDto();

        String requestBody = params.getRequestBody() ;
        String method = StringUtils.hasLength(params.getMethod()) ? params.getMethod() : "GET" ;
        String url = params.getUrl() ;

        Assert.hasLength(url, "调用地址为空");

        Map<String, Object> map = bodyToMap(requestBody) ;
        HashMap<String, String> headers = new HashMap<>();//存放请求头，可以存放多个请求头

        String result= null ;
        if(method.equalsIgnoreCase("GET")){
            HttpUtil.createGet(url).addHeaders(headers).form(map).execute().body();
        }else if(method.equalsIgnoreCase("POST")){
            result= HttpUtil.createPost(url).addHeaders(headers).form(map).execute().body();
        }else if(method.equalsIgnoreCase("PUT")){
            result= HttpUtil.createRequest(Method.PUT, url).addHeaders(headers).execute().body();
        }else if(method.equalsIgnoreCase("DELETE")){
            result= HttpUtil.createRequest(Method.DELETE, url).addHeaders(headers).execute().body();
        }

        log.debug("HttpExecutor execute result : " + result);
    }


}
