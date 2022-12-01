package com.codermast.takeoutfood.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codermast.takeoutfood.common.R;
import com.codermast.takeoutfood.entity.Category;
import com.codermast.takeoutfood.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Description: 分类管理
 * @author: CoderMast
 * @date: 2022/11/26
 * @Blog: <a href="https://www.codermast.com/">codermast</a>
 */
@Slf4j
@RestController
@RequestMapping("/category")
public class CategoryController {
    // 注入CategoryService
    @Autowired
    CategoryService categoryService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * @Description: 获取分类信息
     * @param page 页码
     * @param pageSize 页面大小
     * @Author: CoderMast <a href="https://www.codermast.com/">...</a>
     */
    @GetMapping("/page")
    public R<Page<Category>> page(int page, int pageSize){
        String key = "category:page_" + page + ":pageSize_" + pageSize;
        Page<Category> pageInfo = null;

        ValueOperations opsForValue = redisTemplate.opsForValue();
        pageInfo = (Page<Category>) opsForValue.get(key);

        // 缓存命中
        if (pageInfo != null){
            return R.success(pageInfo);
        }

        pageInfo = new Page<>(page,pageSize);

        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        // 增加排序条件
        queryWrapper.orderByDesc(Category::getSort);

        categoryService.page(pageInfo,queryWrapper);

        // 添加缓存
        opsForValue.set(key,pageInfo,60, TimeUnit.MINUTES);
        log.info(pageInfo.toString());
        return R.success(pageInfo);
    }

    /**
     * @Description: 增加分类
     * @param category 分类封装对象
     *  category.name 分类名称
     *  category.sort 分类排序
     *  category.type 分类类型 1为菜品类型 2为套餐类型
     * @Author: CoderMast <a href="https://www.codermast.com/">...</a>
     */
    @PostMapping
    public R<String> save(@RequestBody Category category){

        if (category == null){
            return R.error("类型为空");
        }

        categoryService.save(category);

        // 增加缓存
        redisTemplate.opsForValue().set("category:" + category.getId(),category,60,TimeUnit.MINUTES);
        return R.success("创建成功");
    }

    /**
     * @Description: 根据id删除分类
     * @param ids 分类id
     * @Author: CoderMast <a href="https://www.codermast.com/">...</a>
     */
    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids){
        // 直接通过id进行删除，未判断是否含有所关联的dish内容
        //categoryService.removeById(ids);

        // 对于上述的优化
        categoryService.removeBatchByIds(ids);

        // 删除缓存
        for (Long id : ids) {
            redisTemplate.delete("category:" + id);
        }

        return R.success("删除成功");
    }

    /**
     * @Description: 更新分类信息
     * @param category 更新分类封装对象
     * @Author: CoderMast <a href="https://www.codermast.com/">...</a>
     */
    @PutMapping
    public R<String> update(@RequestBody Category category){
        if (category == null){
            return R.error("类型为空");
        }

        categoryService.updateById(category);

        // 更新缓存
        ValueOperations opsForValue = redisTemplate.opsForValue();
        opsForValue.set("category:" + category.getId(),category);
        return R.success("更新成功");
    }

    /**
     * @Description: 获取菜品的分类类列表
     * @param type 类型
     * @Author: CoderMast <a href="https://www.codermast.com/">...</a>
     */
    @GetMapping("/list")
    public R<List<Category>> list(Integer type){
        List<Category> list = null;
        String key = "category:dish:" + type;

        ValueOperations opsForValue = redisTemplate.opsForValue();
        list = (List<Category>) opsForValue.get(key);

        // 缓存命中
        if (list != null){
            return R.success(list);
        }

        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(type != null,Category::getType,type);
        queryWrapper.orderByDesc(Category::getSort);

        list = categoryService.list(queryWrapper);

        // 添加缓存
        opsForValue.set(key,list,60,TimeUnit.MINUTES);

        return R.success(list);
    }
}
