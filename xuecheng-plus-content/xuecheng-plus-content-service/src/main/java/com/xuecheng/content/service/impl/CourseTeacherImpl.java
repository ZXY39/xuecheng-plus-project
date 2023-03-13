package com.xuecheng.content.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.content.mapper.CourseTeacherMapper;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CourseTeacherImpl implements CourseTeacherService {
    @Autowired
    CourseTeacherMapper courseTeacherMapper;
    @Override
    public List<CourseTeacher> list(Long courseId) {
        LambdaQueryWrapper<CourseTeacher> courseTeacherWrapper = new LambdaQueryWrapper<>();
        courseTeacherWrapper.eq(CourseTeacher::getCourseId,courseId);
        List<CourseTeacher> courseTeachers = courseTeacherMapper.selectList(courseTeacherWrapper);
        return courseTeachers;
    }

    @Override
    public void add(CourseTeacher courseTeacher) {
        CourseTeacher courseTeacher1 = courseTeacherMapper.selectById(courseTeacher.getId());
        if(courseTeacher1==null){
            courseTeacher.setCreateDate(LocalDateTime.now());
            courseTeacherMapper.insert(courseTeacher);
        }else {
            courseTeacherMapper.updateById(courseTeacher);
        }

    }

    @Override
    public void delete(Long courseId, Long teacherId) {
        LambdaQueryWrapper<CourseTeacher> courseTeacherWrapper = new LambdaQueryWrapper<>();
        courseTeacherWrapper.eq(CourseTeacher::getId,teacherId);
        courseTeacherWrapper.eq(CourseTeacher::getCourseId,courseId);
        courseTeacherMapper.delete(courseTeacherWrapper);
    }
}
