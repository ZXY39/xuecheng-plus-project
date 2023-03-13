package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.service.TeachplanService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@Api(value = "课程计划编辑接口",tags = "课程计划编辑接口")
@RestController
public class TeachplanController {
    @Autowired
    TeachplanService teachplanService;
    @ApiOperation("查询课程计划树形结构")
    @GetMapping("/teachplan/{courseId}/tree-nodes")
    public List<TeachplanDto> getTreeNodes(@PathVariable Long courseId){
        List<TeachplanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);
        return teachplanTree;
    }

    @ApiOperation("课程计划创建或修改")
    @PostMapping("/teachplan")
    public void saveTeachplan( @RequestBody SaveTeachplanDto teachplan){
        teachplanService.saveTeachplan(teachplan);
    }

    @DeleteMapping("/teachplan/{teachplanId}")
    public void deleteTeachplan( @PathVariable Long teachplanId){
        teachplanService.deleteTeachplan(teachplanId);
    }

    @PostMapping("/teachplan/movedown/{teachplanId}")
    public void movedown(@PathVariable Long teachplanId){
        teachplanService.moveDown(teachplanId);
    }
    @PostMapping("/teachplan/moveup/{teachplanId}")
    public void moveup(@PathVariable Long teachplanId){
        teachplanService.moveUp(teachplanId);
    }
}
