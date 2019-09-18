package com.leyou.item.mapper;

import com.leyou.item.pojo.Brand;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.additional.idlist.SelectByIdListMapper;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BrandMapper extends Mapper<Brand>, SelectByIdListMapper<Brand, Long> {

    @Insert("insert into tb_category_brand (brand_id, category_id) values (#{bid}, #{cid})")
    void insertBrandCategory(@Param("cid") Long cid, @Param("bid") Long bid);

    @Select("select a.* from tb_brand a LEFT JOIN tb_category_brand cb on a.id=cb.brand_id WHERE cb.category_id=#{cid}")
    List<Brand> selectByCid(Long cid);
}
