package com.xuecheng.content.api;


import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(value = "教师编辑接口",tags = "教师编辑接口")
@RestController
public class CourseTeacherController {

    @Autowired
    CourseTeacherService courseTeacherService;

    @GetMapping("/courseTeacher/list/{courseId}")
    public List<CourseTeacher> list(@PathVariable Long courseId){
        List<CourseTeacher> list = courseTeacherService.list(courseId);
        return list;
    }

    @PostMapping("/courseTeacher")
    public void add(@RequestBody CourseTeacher courseTeacher){
        courseTeacherService.add(courseTeacher);
    }

    @DeleteMapping("/courseTeacher/course/{courseId}/{teacherId}")
    public void delete(@PathVariable Long courseId,@PathVariable Long teacherId){
        courseTeacherService.delete(courseId,teacherId);
    }


}
