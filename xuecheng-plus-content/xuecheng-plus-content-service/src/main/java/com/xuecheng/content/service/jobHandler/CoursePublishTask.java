package com.xuecheng.content.service.jobHandler;


import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.feignclient.CourseIndex;
import com.xuecheng.content.feignclient.SearchServiceClient;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

@Slf4j
@Component
public class CoursePublishTask extends MessageProcessAbstract {
    @Autowired
    CoursePublishService coursePublishService;

    @Autowired
    SearchServiceClient searchServiceClient;

    @Autowired
    CoursePublishMapper  coursePublishMapper;
    @XxlJob("CoursePublishJobHandler")
    public void coursePublishJobHandler() throws Exception {
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        process(shardIndex,shardTotal,"course_publish",30,60);

    }

    @Override
    public boolean execute(MqMessage mqMessage) {
        Long courseId = Long.valueOf(mqMessage.getBusinessKey1());
        generateCourseHtml(mqMessage,courseId);

        saveCourseIndex(mqMessage,courseId);

        return true;
    }


    private void generateCourseHtml(MqMessage mqMessage,long courseId){
        Long taskId = mqMessage.getId();
        MqMessageService mqMessageService = getMqMessageService();

        int stageOne = mqMessageService.getStageOne(taskId);
        if(stageOne>0){
            log.debug("课程页面静态化重复处理");
            return;
        }
        File file = coursePublishService.generateCourseHtml(courseId);
        if(file==null){
            XueChengPlusException.cast("生成的静态页面为空");

        }
        coursePublishService.uploadCourseHtml(courseId,file);

        mqMessageService.completedStageOne(taskId);
    }

    private void saveCourseIndex(MqMessage mqMessage,long courseId){
        Long id = mqMessage.getId();
        MqMessageService mqMessageService = getMqMessageService();
        int stageTwo = mqMessageService.getStageTwo(id);
        if(stageTwo>0){
            log.debug("课程索引重复处理");
            return;
        }
        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);
        CourseIndex courseIndex = new CourseIndex();
        BeanUtils.copyProperties(coursePublish,courseIndex);
        Boolean add = searchServiceClient.add(courseIndex);
        if(!add){
            XueChengPlusException.cast("远程调用搜索服务添加课程索引失败");   
        }

        mqMessageService.completedStageTwo(id);
    }
}
