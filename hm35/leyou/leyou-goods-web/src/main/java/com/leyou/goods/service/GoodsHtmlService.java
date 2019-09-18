package com.leyou.goods.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Map;

@Service
public class GoodsHtmlService {

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private GoodsService goodsService;

    public void creatHtml(Long spuId){

        PrintWriter printWriter = null;
        try {
            // 获取模型数据
            Map<String, Object> map = this.goodsService.loadData(spuId);
            // 初始化上下文对象
            Context context = new Context();
            // 把模板数据设置给context
            context.setVariables(map);

            // 创建文件输出流
            printWriter = new PrintWriter(new File("C:\\hm35\\tools\\nginx-1.14.0\\html\\item\\" + spuId + ".html"));

            // 使用模板引擎创建静态页面
            this.templateEngine.process("item", context, printWriter);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (printWriter != null) {
                printWriter.close();
            }
        }
    }

    public void deleteHtml(Long id) {
        File file = new File("C:\\hm35\\tools\\nginx-1.14.0\\html\\item\\" + id + ".html");
        file.deleteOnExit();
    }
}
