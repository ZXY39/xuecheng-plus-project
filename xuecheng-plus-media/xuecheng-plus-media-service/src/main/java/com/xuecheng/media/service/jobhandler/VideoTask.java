package com.xuecheng.media.service.jobhandler;


import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileProcessService;
import com.xuecheng.media.service.MediaFileService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * XxlJob开发示例（Bean模式）
 *
 * 开发步骤：
 *      1、任务开发：在Spring Bean实例中，开发Job方法；
 *      2、注解配置：为Job方法添加注解 "@XxlJob(value="自定义jobhandler名称", init = "JobHandler初始化方法", destroy = "JobHandler销毁方法")"，注解value值对应的是调度中心新建任务的JobHandler属性的值。
 *      3、执行日志：需要通过 "XxlJobHelper.log" 打印执行日志；
 *      4、任务结果：默认任务结果为 "成功" 状态，不需要主动设置；如有诉求，比如设置任务结果为失败，可以通过 "XxlJobHelper.handleFail/handleSuccess" 自主设置任务结果；
 *
 * @author xuxueli 2019-12-11 21:52:51
 */
@Slf4j
@Component
public class VideoTask {

    @Autowired
    MediaFileProcessService mediaFileProcessService;
    @Autowired
    MediaFileService mediaFileService;

    @Value("${videoprocess.ffmpegpath}")
    private String ffmpegpath;

    @XxlJob("videoJobHandler")
    public void shardingJobHandler() throws Exception {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        int processors = Runtime.getRuntime().availableProcessors();

        List<MediaProcess> mediaProcessList = mediaFileProcessService.getMediaProcessList(shardIndex, shardTotal, processors);
        int size = mediaProcessList.size();
        log.debug("取到视频处理任务数:"+size);
        if(size<=0){
            return;
        }
        ExecutorService executorService  = Executors.newFixedThreadPool(size);
        CountDownLatch countDownLatch =new CountDownLatch(size);
        mediaProcessList.forEach(mediaProcess -> {
            executorService.execute(()->{
                try {
                    Long taskId = mediaProcess.getId();
                    boolean b = mediaFileProcessService.startTask(taskId);
                    if(!b){
                        log.debug("抢占任务失败,id={}",taskId);
                        return;
                    }
                    String bucket = mediaProcess.getBucket();
                    String objectName = mediaProcess.getFilePath();
                    String fileId = mediaProcess.getFileId();
                    File file = mediaFileService.downloadFileFromMinIO(bucket, objectName);
                    if(file==null){
                        log.debug("下载视频出错，任务id:{},bucket:{},objectName:{}",taskId,bucket,objectName);
                        mediaFileProcessService.saveProcessFinishStatus(taskId,"3",fileId,null,"下载视频到本地失败");
                        return;
                    }
                    //源avi视频的路径
                    String video_path = file.getAbsolutePath();


                    //转换后mp4文件的名称
                    String mp4_name = fileId+".mp4";
                    File mp4File=null;
                    try {
                        mp4File = File.createTempFile(fileId, ".temp");
                    }catch (IOException e){
                        e.printStackTrace();
                        log.debug("创建临时文件异常,{}",e.getMessage());
                        mediaFileProcessService.saveProcessFinishStatus(taskId,"3",fileId,null,"创建临时文件异常");

                        return;
                    }
                    //转换后mp4文件的路径
                    String mp4_path = mp4File.getAbsolutePath();
                    //创建工具类对象
                    Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpegpath,video_path,mp4_name,mp4_path);
                    //开始视频转换，成功将返回success
                    String result
                    //= videoUtil.generateMp4();
                    ="success";
                    if(!result.equals("success")){
                        log.debug("视频转码失败，原因:{},bucket:{},objectName:{}",result,bucket,objectName);
                        mediaFileProcessService.saveProcessFinishStatus(taskId,"3",fileId,null,result);

                    }
                    boolean b1 = mediaFileService.addMediaFileToMinIO(mp4File.getAbsolutePath(), "video/mp4", bucket, objectName);
                    if(!b1){
                        log.debug("上传mp4到minio失败，taskId:{}",taskId);
                        mediaFileProcessService.saveProcessFinishStatus(taskId,"3",fileId,null,"上传mp4到minio失败");
                        return;
                    }
                    String url = getFilePathByMd5(fileId, ".mp4");

                    mediaFileProcessService.saveProcessFinishStatus(taskId,"2",fileId,url,"创建临时文件异常");


                }finally {
                    countDownLatch.countDown();
                }
            });

        });
        countDownLatch.await(30, TimeUnit.MINUTES);

    }
    private String getFilePathByMd5(String fileMd5,String fileExt){
        return   fileMd5.substring(0,1) + "/" + fileMd5.substring(1,2) + "/" + fileMd5 + "/" +fileMd5 +fileExt;
    }

}
