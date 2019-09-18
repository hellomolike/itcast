package com.leyou.search.pojo;

import com.leyou.common.pojo.PageResult;
import com.leyou.item.pojo.Brand;

import java.util.List;
import java.util.Map;

public class SearchResult extends PageResult {

    private List<Map<String, Object>> categories;

    private List<Brand> brands;

    private List<Map<String, Object>> params;

    public SearchResult() {
    }

    public SearchResult(List<Map<String, Object>> categories, List<Brand> brands, List<Map<String, Object>> params) {
        this.categories = categories;
        this.brands = brands;
        this.params = params;
    }

    public SearchResult(Long total, Integer totalPage, List items, List<Map<String, Object>> categories, List<Brand> brands, List<Map<String, Object>> params) {
        super(total, totalPage, items);
        this.categories = categories;
        this.brands = brands;
        this.params = params;
    }

    public List<Map<String, Object>> getParams() {
        return params;
    }

    public void setParams(List<Map<String, Object>> params) {
        this.params = params;
    }

    public List<Map<String, Object>> getCategories() {
        return categories;
    }

    public void setCategories(List<Map<String, Object>> categories) {
        this.categories = categories;
    }

    public List<Brand> getBrands() {
        return brands;
    }

    public void setBrands(List<Brand> brands) {
        this.brands = brands;
    }
}
