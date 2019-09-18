package com.leyou.search.client;

import com.leyou.common.pojo.PageResult;
import com.leyou.item.pojo.SpuBo;
import com.leyou.item.pojo.SpuDetail;
import com.leyou.search.pojo.Goods;
import com.leyou.search.repository.GoodsRepository;
import com.leyou.search.service.SearchService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GoodsClientTest {

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SearchService searchService;

    @Autowired
    private ElasticsearchTemplate template;

    @Autowired
    private GoodsRepository goodsRepository;

    @Test
    public void testQuery() {
        SpuDetail spuDetail = this.goodsClient.querySpuDetailBySpuId(2l);
        System.out.println(spuDetail.toString());
    }

    @Test
    public void testImport(){
        // 创建索引库
        this.template.createIndex(Goods.class);
        // 添加映射
        this.template.putMapping(Goods.class);

        // 分页参数
        Integer page = 1; // 页码
        Integer size = 100; // 条数
        do {
            // 分页查询spu
            PageResult<SpuBo> pageResult = this.goodsClient.querySpuBoList(null, true, page, size);
            // 获取分页结果集中spu记录
            List<SpuBo> spus = pageResult.getItems();
            // 构建goods集合
            List<Goods> goodsList = new ArrayList<>();
            // 遍历spus集合
            spus.forEach(spu -> {
                try {
                    // 从spu构建goods
                    Goods goods = this.searchService.buildGoods(spu);
                    // 添加到goods集合中
                    goodsList.add(goods);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            // 保存到索引库中
            this.goodsRepository.saveAll(goodsList);
            // 获取当前页的条数，如果当前页的条数不为100，说明最后一页
            size = spus.size();
            // 查询下一页
            page++;
        } while (size == 100);

    }

}