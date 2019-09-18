package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.pojo.PageResult;
import com.leyou.item.mapper.BrandMapper;
import com.leyou.item.pojo.Brand;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class BrandService {

    @Autowired
    private BrandMapper brandMapper;

    public PageResult<Brand> queryBrandPageSortBy(String key, Integer page, Integer rows, String sortBy, Boolean desc) {

        // 构建查询实例
        Example example = new Example(Brand.class);
        Example.Criteria criteria = example.createCriteria();

        // 设置查询条件
        if (StringUtils.isNotBlank(key)) {
            criteria.andLike("name", "%" + key + "%").orEqualTo("letter", key);
        }

        // 设置排序
        if (StringUtils.isNotBlank(sortBy)){
            example.setOrderByClause(sortBy + " " + (desc ? "desc" : "asc"));
        }

        // 设置分页
        PageHelper.startPage(page, rows);

        List<Brand> brands = this.brandMapper.selectByExample(example);
        PageInfo<Brand> pageInfo = new PageInfo<>(brands);

        return new PageResult<>(pageInfo.getTotal(), pageInfo.getPages(), pageInfo.getList());
    }

    @Transactional
    public void save(Brand brand, List<Long> cids) {
        // 先保存品牌
        this.brandMapper.insertSelective(brand);

        // 保存品牌和分类的中间表
        cids.forEach(cid -> {
            this.brandMapper.insertBrandCategory(cid, brand.getId());
        });
    }

    public List<Brand> queryByCid(Long cid) {

        return this.brandMapper.selectByCid(cid);
    }

    public Brand queryById(Long id) {
        return this.brandMapper.selectByPrimaryKey(id);
    }

    public List<Brand> queryBrandsByIds(List<Long> ids) {
        return this.brandMapper.selectByIdList(ids);
    }
}
