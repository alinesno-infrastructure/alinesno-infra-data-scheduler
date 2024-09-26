package com.alinesno.infra.data.scheduler.executor.handle;

import com.alinesno.infra.data.scheduler.executor.BaseExecutorService;
import com.alinesno.infra.data.scheduler.executor.bean.TaskInfoBean;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * 使用GitClone代码
 */
@Slf4j
@Service("checkoutExecutor")
public class CheckoutExecutor extends BaseExecutorService {

    @Override
    public void execute(TaskInfoBean task) {
        log.debug("checkout executor");

        // 远程仓库的 URL
        String remoteUrl = "https://github.com/your-username/your-repo.git";

        // 本地目标目录
        File localPath = new File(getWorkspace(task));

        // 设置认证信息（可选，对于需要认证的仓库）
        CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider("your-username", "your-password");

        try {
            log.debug("开始克隆仓库...");
            // 克隆仓库
            Git.cloneRepository()
                    .setURI(remoteUrl)
                    .setDirectory(localPath)
                    .setCredentialsProvider(credentialsProvider)  // 如果需要认证，设置认证提供者
                    .call();
            log.debug("仓库克隆成功: " + localPath.getAbsolutePath());
        } catch (GitAPIException e) {
            log.error("克隆仓库时发生错误: " , e);
        }
    }
}
