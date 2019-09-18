package com.leyou.search.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyou.item.pojo.*;
import com.leyou.search.client.BrandClient;
import com.leyou.search.client.CategoryClient;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.client.SpecificationClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchRequest;
import com.leyou.search.pojo.SearchResult;
import com.leyou.search.repository.GoodsRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;

@Service
public class SearchService {

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private SpecificationClient specificationClient;

    @Autowired
    private ElasticsearchTemplate template;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public Goods buildGoods(Spu spu) throws IOException {
        // 创建goods实体
        Goods goods = new Goods();

        // 查询分类名称
        List<String> names = this.categoryClient.queryNamesByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));

        // 查询品牌
        Brand brand = this.brandClient.queryBrandById(spu.getBrandId());

        // 查询spu下的sku
        List<Sku> skus = this.goodsClient.querySkusBySpuId(spu.getId());

        // 查询spuDetail
        SpuDetail spuDetail = this.goodsClient.querySpuDetailBySpuId(spu.getId());

        // 查询搜索的规格参数
        List<SpecParam> params = this.specificationClient.queryParams(null, spu.getCid3(), true, null);

        // 组装价格集合
        List<Long> prices = new ArrayList<>();
        // 组装sku集合
        List<Map<String, Object>> skuMapList = new ArrayList<>();
        skus.forEach(sku -> {
            prices.add(sku.getPrice());
            Map<String, Object> map = new HashMap<>();
            map.put("id", sku.getId());
            map.put("title", sku.getTitle());
            map.put("price", sku.getPrice());
            map.put("image", StringUtils.isBlank(sku.getImages()) ? "" : StringUtils.split(sku.getImages(), ",")[0]);
            skuMapList.add(map);
        });

        // 获取通过规格参数值
        Map<String, Object> genericSpecMap = MAPPER.readValue(spuDetail.getGenericSpec(), new TypeReference<Map<String, Object>>() {
        });
        // 获取特殊规格参数值
        Map<String, List<Object>> specialSpecMap = MAPPER.readValue(spuDetail.getSpecialSpec(), new TypeReference<Map<String, List<Object>>>() {
        });
        // 遍历搜索的规格参数，获取对应的值
        Map<String, Object> paramMap = new HashMap<>();
        params.forEach(param -> {
            if (param.getGeneric()) {
                Object value = genericSpecMap.get(param.getId().toString());
                if (param.getNumeric()) {
                    value = chooseSegment(value.toString(), param);
                }
                paramMap.put(param.getName(), value);
            } else {
                List<Object> value = specialSpecMap.get(param.getId().toString());
                paramMap.put(param.getName(), value);
            }
        });

        // 设置基本数据
        goods.setId(spu.getId());
        goods.setCid1(spu.getCid1());
        goods.setCid2(spu.getCid2());
        goods.setCid3(spu.getCid3());
        goods.setBrandId(spu.getBrandId());
        goods.setCreateTime(spu.getCreateTime());
        goods.setSubTitle(spu.getSubTitle());
        // 所有需要被搜索的信息，包含标题，分类，甚至品牌
        goods.setAll(spu.getTitle() + " " + StringUtils.join(names, " ") + " " + brand.getName());
        // 设置sku的价格集合
        goods.setPrice(prices);
        // 设置sku集合，json
        goods.setSkus(MAPPER.writeValueAsString(skuMapList));
        // 设置搜索的规格参数
        goods.setSpecs(paramMap);
        return goods;
    }

    private String chooseSegment(String value, SpecParam p) {
        double val = NumberUtils.toDouble(value);
        String result = "其它";
        // 保存数值段
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if (segs.length == 2) {
                end = NumberUtils.toDouble(segs[1]);
            }
            // 判断是否在范围内
            if (val >= begin && val < end) {
                if (segs.length == 1) {
                    result = segs[0] + p.getUnit() + "以上";
                } else if (begin == 0) {
                    result = segs[1] + p.getUnit() + "以下";
                } else {
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }

    public SearchResult search(SearchRequest request) {
        // 判断关键字是否为null
        if (StringUtils.isBlank(request.getKey())) {
            // 返回默认结果集
            return new SearchResult();
        }
        // 创建一个NativeSearchQueryBuilder
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();

        // 结果集过滤
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id", "skus", "subTitle"}, null));

        // 放入搜索条件
        BoolQueryBuilder basicQuery = buildBasicQueryBuilder(request);
        queryBuilder.withQuery(basicQuery);

        // 放入分页条件
        queryBuilder.withPageable(PageRequest.of(request.getPage() - 1, request.getSize()));

        String categoryAggName = "categories";
        String brandAggName = "brands";
        // 添加分类聚合
        queryBuilder.addAggregation(AggregationBuilders.terms(categoryAggName).field("cid3"));
        // 添加品牌的聚合
        queryBuilder.addAggregation(AggregationBuilders.terms(brandAggName).field("brandId"));

        // 执行查询
        AggregatedPage<Goods> goodsPage = (AggregatedPage<Goods>) this.goodsRepository.search(queryBuilder.build());

        // 解析聚合结果集
        List<Map<String, Object>> categories = getCategroyAgg(goodsPage.getAggregation(categoryAggName));
        List<Brand> brands = getBrandAgg(goodsPage.getAggregation(brandAggName));

        // 判断分类是否唯一
        List<Map<String, Object>> params = new ArrayList<>();
        if (categories.size() == 1) {
            params = getParamAgge((Long) categories.get(0).get("id"), basicQuery);
        }

        // 返回搜索的结果集
        return new SearchResult(goodsPage.getTotalElements(), goodsPage.getTotalPages(), goodsPage.getContent(), categories, brands, params);
    }

    /**
     * 构建查询条件：基本查询，过滤查询
     *
     * @param request
     * @return
     */
    private BoolQueryBuilder buildBasicQueryBuilder(SearchRequest request) {
        // 创建bool查询
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        // 给bool查询添加基本查询条件
        boolQueryBuilder.must(QueryBuilders.matchQuery("all", request.getKey()).operator(Operator.AND));

        // 给bool查询添加过滤
        Map<String, String> filter = request.getFilter();
        if (CollectionUtils.isEmpty(filter)){
            return boolQueryBuilder;
        }
        for (Map.Entry<String, String> entry : filter.entrySet()) {
            String key = entry.getKey();
            if (StringUtils.equals("品牌", entry.getKey())) {
                key = "brandId";
            } else if (StringUtils.equals("分类", entry.getKey())) {
                key = "cid3";
            } else {
                key = "specs." + key + ".keyword";
            }
            boolQueryBuilder.filter(QueryBuilders.termQuery(key, entry.getValue()));
        }
        return boolQueryBuilder;
    }

    /**
     * 根据参数聚合
     *
     * @param cid
     * @param basicQuery
     * @return
     */
    private List<Map<String, Object>> getParamAgge(Long cid, QueryBuilder basicQuery) {
        // 获取所有要聚合的结果集
        List<SpecParam> params = this.specificationClient.queryParams(null, cid, true, null);

        // 构造自定义查询构造器
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        queryBuilder.withQuery(basicQuery);

        params.forEach(param -> {
            queryBuilder.addAggregation(AggregationBuilders.terms(param.getName()).field("specs." + param.getName() + ".keyword"));
        });

        AggregatedPage<Goods> goodsPage = (AggregatedPage<Goods>) this.goodsRepository.search(queryBuilder.build());
        // 获取所有的聚合结果集，key-聚合名称， value-聚合结果
        Map<String, Aggregation> aggregationMap = goodsPage.getAggregations().asMap();

        List<Map<String, Object>> paramMapList = new ArrayList<>();
        for (Map.Entry<String, Aggregation> entry : aggregationMap.entrySet()) {
            StringTerms terms = (StringTerms) entry.getValue();
            Map<String, Object> map = new HashMap<>();
            map.put("k", entry.getKey());
            // 封装到List<String>
            List<String> values = new ArrayList<>();
            terms.getBuckets().forEach(bucket -> {
                values.add(bucket.getKeyAsString());
            });
            map.put("options", values);
            paramMapList.add(map);
        }
        return paramMapList;
    }

    /**
     * 解析品牌的聚合结果集
     *
     * @param aggregation
     * @return
     */
    private List<Brand> getBrandAgg(Aggregation aggregation) {
        List<Long> ids = new ArrayList<>();
        // 强转成长整型的聚合结果集
        LongTerms brandTerms = (LongTerms) aggregation;
        // 获取聚合结果集中桶
        brandTerms.getBuckets().forEach(bucket -> {
            ids.add(bucket.getKeyAsNumber().longValue());
        });
        return this.brandClient.queryBrandsByIds(ids);
    }

    /**
     * 解析分类聚合结果集
     *
     * @param aggregation
     * @return
     */
    private List<Map<String, Object>> getCategroyAgg(Aggregation aggregation) {
        List<Long> cids = new ArrayList<>();
        // 强转成长整型的聚合结果集
        LongTerms categoryTerms = (LongTerms) aggregation;
        // 获取聚合结果集中所有的桶[key:categoryId]
        categoryTerms.getBuckets().forEach(bucket -> {
            cids.add(bucket.getKeyAsNumber().longValue());
        });
        List<Map<String, Object>> categoryMapList = new ArrayList<>();
        if (CollectionUtils.isEmpty(cids)) {
            return categoryMapList;
        }
        List<String> names = this.categoryClient.queryNamesByIds(cids);

        for (int i = 0; i < cids.size(); i++) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", cids.get(i));
            map.put("title", names.get(i));
            categoryMapList.add(map);
        }
        return categoryMapList;
    }

    public void createIndex(Long id) throws IOException {
        Spu spu = this.goodsClient.querySpuById(id);
        Goods goods = this.buildGoods(spu);
        this.goodsRepository.save(goods);
    }

    public void deleteIndex(Long id) {

        this.goodsRepository.deleteById(id);
    }
}
