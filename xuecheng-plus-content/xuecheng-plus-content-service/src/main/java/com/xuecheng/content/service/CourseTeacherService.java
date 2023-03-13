package com.xuecheng.content.service;

import com.xuecheng.content.model.po.CourseTeacher;

import java.util.List;

public interface CourseTeacherService  {

    public List<CourseTeacher> list(Long teacherId);

    public void add(CourseTeacher courseTeacher);

    public void delete(Long courseId, Long teacherId);

}