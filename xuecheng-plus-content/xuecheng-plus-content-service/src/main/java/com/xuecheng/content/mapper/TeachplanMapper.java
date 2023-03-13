package com.xuecheng.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;

import java.util.List;

/**
 * <p>
 * 课程计划 Mapper 接口
 * </p>
 *
 * @author itcast
 */
public interface TeachplanMapper extends BaseMapper<Teachplan> {

        public List<TeachplanDto> selectTreeNodes(long courseId);

        public void decreaseOrderBy(long courseId,long parentid,int orderby);

//        public void moveDownOrderBy(long courseId,long parentid,int NewOrderby);
//        public void moveUpOrderBy(long courseId,long parentid,int NewOrderby);
        public void deleteNodes(long parentid);


}
