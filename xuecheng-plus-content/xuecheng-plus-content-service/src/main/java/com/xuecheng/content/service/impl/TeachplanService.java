package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.CommonError;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service

public class TeachplanService implements com.xuecheng.content.service.TeachplanService {
    @Autowired
    TeachplanMapper teachplanMapper;
    @Autowired
    TeachplanMediaMapper teachplanMediaMapper;
    @Override
    public List<TeachplanDto> findTeachplanTree(long courseId) {
        List<TeachplanDto> teachplanDtos = teachplanMapper.selectTreeNodes(courseId);
        return teachplanDtos;
    }

    @Override
    public void saveTeachplan(SaveTeachplanDto saveTeachplanDto) {
        Long teachplanId = saveTeachplanDto.getId();
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        if(teachplan==null){
            teachplan = new Teachplan();
            BeanUtils.copyProperties(saveTeachplanDto,teachplan);
            Long parentid = saveTeachplanDto.getParentid();
            Long courseId = saveTeachplanDto.getCourseId();
            Integer teachplanCount = getTeachplanCount(parentid, courseId);
            teachplan.setOrderby(teachplanCount);
            teachplanMapper.insert(teachplan);
        }else {
            BeanUtils.copyProperties(saveTeachplanDto,teachplan);
            teachplanMapper.updateById(teachplan);
        }

    }
    @Override
    @Transactional
    public void deleteTeachplan(Long teachplanId) {
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        Long parentid = teachplan.getParentid();
        Integer orderby = teachplan.getOrderby();
        int i = teachplanMapper.deleteById(teachplanId);
        if(i<=0){
            XueChengPlusException.cast("删除失败");
        }
        Long courseId = teachplan.getCourseId();
        teachplanMapper.decreaseOrderBy(courseId,parentid,orderby);
        if(teachplan.getParentid()==0){
            teachplanMapper.deleteNodes(teachplanId);
        }
    }

    @Override
    public void moveUp(Long teachplanId) {
        move(teachplanId,-1);
    }

    @Override
    public void moveDown(Long teachplanId) {
        move(teachplanId,1);
    }

    @Transactional
    @Override
    public TeachplanMedia associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto) {
        Long teachplanId = bindTeachplanMediaDto.getTeachplanId();
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        if(teachplan==null){
            XueChengPlusException.cast("课程计划不存在");
        }


        int delete = teachplanMediaMapper.delete(new LambdaQueryWrapper<TeachplanMedia>()
                .eq(TeachplanMedia::getTeachplanId, bindTeachplanMediaDto.getTeachplanId()));

        TeachplanMedia teachplanMedia = new TeachplanMedia();
        BeanUtils.copyProperties(bindTeachplanMediaDto,teachplanMedia);
        teachplanMedia.setCourseId(teachplan.getCourseId());
        teachplanMedia.setMediaFilename(bindTeachplanMediaDto.getFileName());
        teachplanMediaMapper.insert(teachplanMedia);

        return null;
    }

    private void move(Long teachplanId,int ud){
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        Long parentid = teachplan.getParentid();
        Long courseId = teachplan.getCourseId();
        Integer orderby = teachplan.getOrderby();

        if(orderby==0 &&ud==-1){
            return;
        }
        Integer teachplanCount = getTeachplanCount(parentid, courseId);
        if(orderby.equals(teachplanCount-1) &&ud==1){
            return;
        }
        LambdaQueryWrapper<Teachplan> teachplanWrapper = new LambdaQueryWrapper<>();
        teachplanWrapper.eq(Teachplan::getParentid,parentid);
        teachplanWrapper.eq(Teachplan::getCourseId,courseId);
        teachplanWrapper.eq(Teachplan::getOrderby,orderby+ud);
        Teachplan teachplanOld = teachplanMapper.selectOne(teachplanWrapper);

        teachplan.setOrderby(teachplan.getOrderby()+ud);
        teachplanOld.setOrderby(teachplan.getOrderby()-ud);
        teachplanMapper.updateById(teachplan);
        teachplanMapper.updateById(teachplanOld);
//        Long parentid = teachplan.getParentid();
//        Long courseId = teachplan.getCourseId();
//        Integer orderby = teachplan.getOrderby();
//        if(ud<0){
//            teachplanMapper.moveUpOrderBy(courseId,parentid,orderby);
//            teachplanMapper.moveDownOrderBy(courseId,parentid,orderby+ud);
//        }else {
//            teachplanMapper.moveDownOrderBy(courseId,parentid,orderby);
//            teachplanMapper.moveUpOrderBy(courseId,parentid,orderby+ud);
//        }
    }
    private Integer getTeachplanCount(Long parentid, Long courseId) {
        LambdaQueryWrapper<Teachplan> teachplanWrapper = new LambdaQueryWrapper<>();
        teachplanWrapper.eq(Teachplan::getCourseId, courseId).eq(Teachplan::getParentid, parentid);

        return teachplanMapper.selectCount(teachplanWrapper);
    }
}
