package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;

import java.util.List;

public interface TeachplanService {
    public List<TeachplanDto> findTeachplanTree(long courseId);

    public void saveTeachplan(SaveTeachplanDto saveTeachplanDto);

    public void deleteTeachplan(Long teachplanId);

    public void moveUp(Long teachplanId);

    public void moveDown(Long teachplanId);
}
