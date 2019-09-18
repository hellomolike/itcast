package com.leyou.item.controller;

import com.leyou.common.pojo.PageResult;
import com.leyou.item.pojo.Brand;
import com.leyou.item.service.BrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("brand")
public class BrandController {

    @Autowired
    private BrandService brandService;

    @GetMapping("page")
    public ResponseEntity<PageResult<Brand>> queryBrandsByPageSortBy(
            @RequestParam(value = "key", required = false) String key,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "rows", defaultValue = "5") Integer rows,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "desc", required = false) Boolean desc
    ){
        PageResult<Brand> pageResult = this.brandService.queryBrandPageSortBy(key, page, rows, sortBy, desc);
        if (CollectionUtils.isEmpty(pageResult.getItems())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(pageResult);
    }

    @PostMapping
    public ResponseEntity<Void> saveBrand(Brand brand, @RequestParam("cids") List<Long> cids){

        this.brandService.save(brand, cids);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("cid/{cid}")
    public ResponseEntity<List<Brand>> queryBrandsByCid(@PathVariable("cid")Long cid){
        List<Brand> brands = this.brandService.queryByCid(cid);
        if (CollectionUtils.isEmpty(brands)){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(brands);
    }

    @GetMapping("{id}")
    public ResponseEntity<Brand> queryBrandById(@PathVariable("id")Long id){
        Brand brand = this.brandService.queryById(id);
        if (brand == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(brand);
    }

    @GetMapping("query")
    public ResponseEntity<List<Brand>> queryBrandsByIds(@RequestParam("ids") List<Long> ids){
        List<Brand> brands = this.brandService.queryBrandsByIds(ids);
        if (CollectionUtils.isEmpty(brands)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(brands);
    }
}
