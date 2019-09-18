package com.leyou.item.service;

import com.leyou.item.mapper.SpecGroupMapper;
import com.leyou.item.mapper.SpecParamMapper;
import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpecificationService {

    @Autowired
    private SpecGroupMapper specGroupMapper;

    @Autowired
    private SpecParamMapper specParamMapper;

    public List<SpecGroup> queryGroupsByCid(Long cid) {

        SpecGroup record = new SpecGroup();
        record.setCid(cid);
        return this.specGroupMapper.select(record);
    }

    public List<SpecParam> queryParams(Long gid, Long cid, Boolean searching, Boolean generic) {
        SpecParam record = new SpecParam();
        record.setGroupId(gid);
        record.setCid(cid);
        record.setSearching(searching);
        record.setGeneric(generic);
        return this.specParamMapper.select(record);
    }

    public List<SpecGroup> queryGroupByCid(Long cid) {

        SpecGroup record = new SpecGroup();
        record.setCid(cid);
        // 根据分类查询所有的组
        List<SpecGroup> groups = this.specGroupMapper.select(record);
        groups.forEach(group -> {
            SpecParam param = new SpecParam();
            param.setGroupId(group.getId());
            // 根据组id查询该组下的所有规格参数
            group.setParams(this.specParamMapper.select(param));
        });

        return groups;
    }
}
