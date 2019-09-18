package com.leyou.goods.controller;

import com.leyou.goods.service.GoodsHtmlService;
import com.leyou.goods.service.GoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@Controller
public class GoodsController {

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private GoodsHtmlService goodsHtmlService;

    @GetMapping("item/{spuId}.html")
    public String toItem(@PathVariable("spuId")Long spuId, Model model){
        // 通过service方法，组合数据模型
        Map<String, Object> map = this.goodsService.loadData(spuId);
        // 把数据模型组合响应到页面
        model.addAllAttributes(map);

        this.goodsHtmlService.creatHtml(spuId);
        // 跳转到item页面
        return "item";
    }
}
