package com.alinesno.infra.data.scheduler.executor.handle;

import com.alibaba.fastjson.JSONObject;
import com.alinesno.infra.data.scheduler.api.ParamsDto;
import com.alinesno.infra.data.scheduler.executor.AbstractExecutorService;
import com.alinesno.infra.data.scheduler.executor.bean.TaskInfoBean;
import com.alinesno.infra.data.scheduler.executor.bean.WechatMessage;
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

import static com.alinesno.infra.common.core.constants.Constants.SUCCESS;

/**
 * 通知接口执行，目前只支持企业微信
 */
@Slf4j
@Service("noticeExecutor")
public class NoticeExecutor extends AbstractExecutorService {

    private static final int MAX_LENGTH = 1024;

    public void execute(TaskInfoBean task){
        // 发送企业微信通知
        log.info("noticeExecutor execute");

        ParamsDto params = getParamsDto() ;
        String imType = params.getImType() ;
        String email = params.getEmail();
        String wechatKey =  params.getWechatKey() ;
        String noticeContent = params.getNoticeContent() ;

        if(imType.equals("email")){
            String result = sendEmail(email , noticeContent) ;
            log.debug("邮件通知结果：" + result);
        }else if(imType.equals("wechat_work")){
            String result = sendWechatWork(wechatKey , noticeContent) ;
            log.debug("企业微信通知结果：" + result);
        }
    }

    /**
     * 发送邮件通知
     */
    public String sendEmail(String email , String textMsg) {
        log.info("发送邮件通知：" + email + " , " + textMsg);
        return null ;
    }

    /**
     * 发送企业微信
     * @param key
     * @param textMsg
     * @return
     */
    @SneakyThrows
    public String sendWechatWork(String key , String textMsg) {

        WechatMessage message = WechatMessage.type("markdown").markdown(truncateText(textMsg));
        String json = JSONObject.toJSONString(message);
        log.debug("发送企业微信消息：" + json);


        CloseableHttpClient httpClient = HttpClients.createDefault();//实例化对象
        HttpPost httpPost = new HttpPost("https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=" + key);//url地址
        httpPost.addHeader("Content-Type", "application/json; charset=utf-8");//发送消息的格式;
        StringEntity se = new StringEntity(json, "utf-8");//编码转换
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

        return SUCCESS ;
    }

    /**
     * 截取文本消息，避免超过企业微信限制
     * @param textMsg
     * @return
     */
    public static String truncateText(String textMsg) {
        if (textMsg == null) {
            throw new IllegalArgumentException("textMsg cannot be null");
        }

        int length = textMsg.length();

        if (length > MAX_LENGTH) {
            // 截取字符
            textMsg = textMsg.substring(0, MAX_LENGTH);
        }

        return textMsg;
    }

}
