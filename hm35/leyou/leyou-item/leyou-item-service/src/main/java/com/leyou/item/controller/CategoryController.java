package com.leyou.item.controller;

import com.leyou.item.pojo.Category;
import com.leyou.item.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * restful interface
     * response status: get(200) put/delete(204) post(201)
     * not found: 404
     * parameter error: 400
     * server error: 500
     * @param pid
     * @return
     */
    @GetMapping("list")
    public ResponseEntity<List<Category>> queryCategoriesByPid(@RequestParam("pid")Long pid){
        try {
            if (pid == null || pid.longValue() < 0){
                // responsestatus=400
                // return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
                // return new ResponseEntity(HttpStatus.BAD_REQUEST);
                return ResponseEntity.badRequest().build();
            }
            // int i=1/0;
            List<Category> categories = this.categoryService.queryByPid(pid);
            // resources not found ,return 404
            if (CollectionUtils.isEmpty(categories)) {
                // return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                return ResponseEntity.notFound().build();
            }
            // response status :200
            // return ResponseEntity.status(HttpStatus.OK).body(categories);
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // server error: 500
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    @GetMapping("query")
    public ResponseEntity<List<String>> queryNamesByIds(@RequestParam("ids") List<Long> ids){
        List<String> names = this.categoryService.queryNamesByIds(ids);
        if (CollectionUtils.isEmpty(names)) {
            // return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            return ResponseEntity.notFound().build();
        }
        // response status :200
        // return ResponseEntity.status(HttpStatus.OK).body(categories);
        return ResponseEntity.ok(names);
    }

}
