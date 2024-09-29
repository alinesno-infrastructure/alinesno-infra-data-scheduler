package com.alinesno.infra.data.scheduler.executor.handle;

import com.alinesno.infra.data.scheduler.api.ParamsDto;
import com.alinesno.infra.data.scheduler.executor.BaseExecutorService;
import com.alinesno.infra.data.scheduler.executor.bean.TaskInfoBean;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Http执行器
 */
@Slf4j
@Service("httpExecutor")
public class HttpExecutor extends BaseExecutorService {

    @Override
    public void execute(TaskInfoBean taskInfo) {
        log.debug("HttpExecutor execute");

        ParamsDto params = getParamsDto(taskInfo);

        int retryCount = params.getRetryCount() ; // 重试次数
        String method = params.getMethod() ;
        String url = params.getUrl() ;

        Callable<Response> callable = new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                log.info("重试调用");

                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(url)
                        .method(method, null)
                        .build();

                return client.newCall(request).execute();
            }
        };

        Response result =  graceRetry(callable , retryCount);
        log.debug("HttpExecutor execute result : " + result);
    }

    public Response graceRetry(Callable<Response> callable, int retryCount) {
        Retryer<Response> retryer = RetryerBuilder.<Response>newBuilder()
                .retryIfException()		// 当发生异常时重试
                .retryIfResult(response -> response.code() != 200)		// 当返回码不为200时重试
                .withWaitStrategy(WaitStrategies.fibonacciWait(1000, 10, TimeUnit.SECONDS))	// 等待策略：使用斐波拉契数列递增等待
                .withStopStrategy(StopStrategies.stopAfterAttempt(retryCount))		// 重试达到10次时退出
                .build();
        try {
            return retryer.call(callable);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        throw new RuntimeException("重试失败");
    }

}
