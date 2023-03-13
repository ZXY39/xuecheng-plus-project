package com.xuecheng.content;

import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.service.CourseCategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.xml.ws.Action;
import java.util.List;

@SpringBootTest
public class CourseCategoryServiceTests {
    @Autowired
    CourseCategoryService categoryService;
    @Test
    public void test(){
        List<CourseCategoryTreeDto> courseCategoryTreeDtos =
                categoryService.queryTreeNodes("1");
        System.out.println(courseCategoryTreeDtos);
    }
}
