package com.leyou.item.service;

import com.leyou.item.mapper.CategoryMapper;
import com.leyou.item.pojo.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    /**
     * query by parentId
     * @param pid
     * @returnsfdd
     */
    public List<Category> queryByPid(Long pid) {
        Category category = new Category();
        category.setParentId(pid);
        return this.categoryMapper.select(category);
    }

    public List<String> queryNamesByIds(List<Long> ids) {

        List<Category> categories = this.categoryMapper.selectByIdList(ids);

        return categories.stream().map(category -> category.getName()).collect(Collectors.toList());
    }
}
