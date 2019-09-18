package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.pojo.PageResult;
import com.leyou.item.mapper.*;
import com.leyou.item.pojo.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GoodsService {

    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private SpuDetailMapper spuDetailMapper;

    @Autowired
    private BrandMapper brandMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private AmqpTemplate amqpTemplate;

    public PageResult<SpuBo> querySpuBoList(String key, Boolean saleable, Integer page, Integer rows) {

        // 设置分页参数
        PageHelper.startPage(page, rows);

        // 设置查询条件
        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(key)) {
            criteria.andLike("title", "%" + key + "%");
        }
        criteria.andEqualTo("saleable", saleable);

        // 执行查询
        List<Spu> spus = this.spuMapper.selectByExample(example);

        List<SpuBo> spuBoList = spus.stream().map(spu -> {
            SpuBo spuBo = new SpuBo();
            // 属性copy
            BeanUtils.copyProperties(spu, spuBo);

            // 设置分类名称
            List<String> names = this.categoryService.queryNamesByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
            spuBo.setCname(StringUtils.join(names, "/"));

            // 设置品牌名称
            Brand brand = this.brandMapper.selectByPrimaryKey(spu.getBrandId());
            spuBo.setBname(brand.getName());

            return spuBo;
        }).collect(Collectors.toList());

        PageInfo<Spu> pageInfo = new PageInfo<>(spus);
        return new PageResult<>(pageInfo.getTotal(), pageInfo.getPages(), spuBoList);
    }

    @Transactional
    public void save(SpuBo spuBo) {
        // 新增spu
        Spu spu = new Spu();
        BeanUtils.copyProperties(spuBo, spu);
        // 一些字段赋值
        spu.setSaleable(true);
        spu.setValid(true);
        spu.setCreateTime(new Date());
        spu.setLastUpdateTime(spu.getCreateTime());
        Boolean b = this.spuMapper.insertSelective(spu) == 1;

        // 新增spuDetail
        if (b) {
            SpuDetail spuDetail = spuBo.getSpuDetail();
            spuDetail.setSpuId(spu.getId());
            this.spuDetailMapper.insertSelective(spuDetail);
        }
        saveSkuAndStock(spuBo, spu.getId());

        sendMsg("insert", spu.getId());
    }

    private void sendMsg(String type, Long spuId) {
        try {
            this.amqpTemplate.convertAndSend("LEYOU_ITEM_EXCHANGE", "item." + type, spuId);
        } catch (AmqpException e) {
            e.printStackTrace();
        }
    }

    private void saveSkuAndStock(SpuBo spuBo, Long spuId) {
        // 新增skus
        List<Sku> skus = spuBo.getSkus();
        skus.forEach(sku -> {
            sku.setSpuId(spuId);
            sku.setCreateTime(new Date());
            sku.setLastUpdateTime(sku.getCreateTime());
            this.skuMapper.insertSelective(sku);

            // 新增stock
            Stock stock = new Stock();
            stock.setStock(sku.getStock());
            stock.setSkuId(sku.getId());
            this.stockMapper.insertSelective(stock);
        });
    }

    public SpuDetail querySpuDetailBySpuId(Long spuId) {

        return this.spuDetailMapper.selectByPrimaryKey(spuId);
    }

    public List<Sku> querySkusBySpuId(Long spuId) {

        // 查询skus
        Sku sku = new Sku();
        sku.setSpuId(spuId);
        List<Sku> skus = this.skuMapper.select(sku);
        // 查询每条sku中的库存信息
        skus.forEach(s -> {
            Stock stock = this.stockMapper.selectByPrimaryKey(s.getId());
            s.setStock(stock.getStock());
        });
        return skus;
    }

    public void update(SpuBo spuBo) {

        // 根据spuId查询所属的skus
        List<Sku> skus = this.querySkusBySpuId(spuBo.getId());
        // 获取skus中多有ids
        List<Long> skuIds = skus.stream().map(sku -> sku.getId()).collect(Collectors.toList());

        // 先删除stock
        Example example = new Example(Stock.class);
        example.createCriteria().andIn("skuId", skuIds);
        this.stockMapper.deleteByExample(example);

        // 再删除sku
        Example examp = new Example(Sku.class);
        examp.createCriteria().andIn("id", skuIds);
        this.skuMapper.deleteByExample(examp);

        // 新增sku和stock
        saveSkuAndStock(spuBo, spuBo.getId());

        // 更新 spu 和spuDetail
        spuBo.setLastUpdateTime(new Date());
        spuBo.setSaleable(true);
        spuBo.setValid(true);
        this.spuMapper.updateByPrimaryKeySelective(spuBo);

        this.spuDetailMapper.updateByPrimaryKeySelective(spuBo.getSpuDetail());

        sendMsg("update", spuBo.getId());
    }

    public Spu querySpuById(Long id) {
        return this.spuMapper.selectByPrimaryKey(id);
    }
}
