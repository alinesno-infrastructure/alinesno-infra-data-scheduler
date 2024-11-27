package com.alinesno.infra.data.scheduler.executor.handle;

import com.alinesno.infra.data.scheduler.api.ParamsDto;
import com.alinesno.infra.data.scheduler.executor.AbstractExecutorService;
import com.alinesno.infra.data.scheduler.executor.bean.TaskInfoBean;
import com.alinesno.infra.data.scheduler.executor.utils.GitRepositoryUtils;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.lang.exception.RpcServiceRuntimeException;
import java.io.File;
import java.util.Arrays;
import java.util.Objects;

/**
 * 使用GitClone代码
 */
@Slf4j
@Service("checkoutExecutor")
public class CheckoutExecutor extends AbstractExecutorService {

    @Override
    public void execute(TaskInfoBean task) {
        configTaskParams(task, this);
        log.debug("checkout executor");

        ParamsDto paramsDto = getParamsDto() ;

        String remoteUrl = paramsDto.getGitUrl() ;
        String branch = paramsDto.getGitBranch() ;
        File localPath = new File(getWorkspace() , Objects.requireNonNull(GitRepositoryUtils.getRepositoryNameFromUrl(remoteUrl)));

        Assert.hasLength(remoteUrl , "地址不能为空");
        Assert.hasLength(branch , "分支不能为空");

        try {
            writeLog("开始克隆仓库:" + remoteUrl +",分支:" + branch);
            // 克隆仓库
            CloneCommand cloneCommand = Git.cloneRepository()
                    .setDirectory(localPath)
                    .setDepth(1)
                    .setURI(remoteUrl)
                    .setBranch(branch);

            if( paramsDto.getGitUsername() != null && !paramsDto.getGitUsername().isEmpty()){
                CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(paramsDto.getGitUsername(), paramsDto.getGitPassword());
                cloneCommand.setCredentialsProvider(credentialsProvider) ;
            }

            Git git = cloneCommand.call();
            writeLog("仓库: " +git.getRepository()+ "  克隆成功") ;

            File[] gitFiles = localPath.listFiles() ;
            if( gitFiles != null){
                Arrays.stream(gitFiles).forEach(file -> writeLog("-->> 文件: " + file.getName()));
            }

        } catch (GitAPIException e) {
            log.error("克隆仓库时发生错误: " , e);
            writeLog(e);
            throw new RpcServiceRuntimeException(e.getMessage());
        }
    }
}
