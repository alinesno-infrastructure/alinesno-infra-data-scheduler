package com.alinesno.infra.data.scheduler.executor.handle;

import com.alinesno.infra.data.scheduler.api.ParamsDto;
import com.alinesno.infra.data.scheduler.executor.BaseExecutorService;
import com.alinesno.infra.data.scheduler.executor.bean.TaskInfoBean;
import com.alinesno.infra.data.scheduler.executor.utils.GitRepositoryUtils;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;

/**
 * 使用GitClone代码
 */
@Slf4j
@Service("checkoutExecutor")
public class CheckoutExecutor extends BaseExecutorService {

    @Override
    public void execute(TaskInfoBean task) {
        log.debug("checkout executor");

        ParamsDto paramsDto = getParamsDto(task) ;

        String remoteUrl = paramsDto.getGitUrl() ;
        String branch = paramsDto.getGitBranch() ;
        File localPath = new File(getWorkspace(task) , Objects.requireNonNull(GitRepositoryUtils.getRepositoryNameFromUrl(remoteUrl)));


        try {
            writeLog(task , "开始克隆仓库...");
            // 克隆仓库
            CloneCommand cloneCommand = Git.cloneRepository()
                    .setURI(remoteUrl)
                    .setBranch(branch)
                    .setDirectory(localPath) ;

            if( paramsDto.getGitUsername() != null && !paramsDto.getGitUsername().isEmpty()){
                CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(paramsDto.getGitUsername(), paramsDto.getGitPassword());
                cloneCommand.setCredentialsProvider(credentialsProvider) ;
            }

            Git git = cloneCommand.call();
            writeLog(task , "仓库: " +remoteUrl+ "  克隆成功:" + localPath.getAbsolutePath());

            File[] gitFiles = localPath.listFiles() ;
            Arrays.stream(gitFiles).forEach(file -> writeLog(task , "-->> 文件: " + file.getName()));

        } catch (GitAPIException e) {
            log.error("克隆仓库时发生错误: " , e);
        }
    }
}
