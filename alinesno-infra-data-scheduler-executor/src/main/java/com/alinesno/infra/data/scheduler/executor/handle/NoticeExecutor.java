package com.alinesno.infra.data.scheduler.executor.handle;

import com.alinesno.infra.data.scheduler.api.ParamsDto;
import com.alinesno.infra.data.scheduler.executor.BaseExecutorService;
import com.alinesno.infra.data.scheduler.executor.bean.TaskInfoBean;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

/**
 * 通知接口执行，目前只支持企业微信
 */
@Slf4j
@Service("noticeExecutor")
public class NoticeExecutor extends BaseExecutorService {

    public void execute(TaskInfoBean task){
        // 发送企业微信通知
        log.info("noticeExecutor execute");

        ParamsDto params = getParamsDto(task) ;
        String wechatKey =  params.getWechatKey() ;

        String result = send(wechatKey , params.getName()+":" + params.getDesc()) ;
        log.debug("企业微信通知结果：" + result);
    }

    @SneakyThrows
    public String send(String key , String textMsg) {

        String content =   "{\n" +
                "   \"msgtype\": \"text\",\n" +
                "   \"text\": {\n" +
                "    \"content\": \"" + textMsg + "\"\n" +
                "  }\n" +
                "}";

        CloseableHttpClient httpClient = HttpClients.createDefault();//实例化对象
        HttpPost httpPost = new HttpPost("https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=" + key);//url地址
        httpPost.addHeader("Content-Type", "application/json; charset=utf-8");//发送消息的格式;
        StringEntity se = new StringEntity(content, "utf-8");//编码转换
        httpPost.setEntity(se);

        CloseableHttpResponse response = httpClient.execute(httpPost);

        //发送成功接收返回值
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            String result = EntityUtils.toString(response.getEntity(), "utf-8");
            log.debug("发送微信机器人消息成功 " + result);
            return result;
        } else {
            log.debug("发送微信机器人消息失败");
        }
        // 关闭
        httpClient.close();
        response.close();

        return "发送微信机器人消息失败";
    }

}
