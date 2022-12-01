package com.codermast.takeoutfood.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codermast.takeoutfood.common.R;
import com.codermast.takeoutfood.dto.DishDto;
import com.codermast.takeoutfood.entity.Category;
import com.codermast.takeoutfood.entity.Dish;
import com.codermast.takeoutfood.entity.DishFlavor;
import com.codermast.takeoutfood.service.CategoryService;
import com.codermast.takeoutfood.service.DishFlavorService;
import com.codermast.takeoutfood.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Description: 菜品管理控制器
 * @author: CoderMast
 * @date: 2022/11/27
 * @Blog: <a href="https://www.codermast.com/">codermast</a>
 */
@Slf4j
@RestController
@RequestMapping("/dish")
public class DishController {
    // 注入dish业务实体
    @Autowired
    private DishService dishService;

    @Autowired
    private DishFlavorService dishFlavorService;

    // 注入category实体
    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * @param page     页码
     * @param pageSize 页面大小
     * @param name     关键词
     * @Description: 分页查询dish内容
     * @Author: <a href="https://www.codermast.com/">CoderMast</a>
     */
    @GetMapping("/page")
    public R<Page<DishDto>> page(int page, int pageSize, String name) {

        Page<DishDto> dishDtoPage = null;
        String key = "page_" + page + ":pageSize_" + pageSize + ":name_" + name;
        ValueOperations opsForValue = redisTemplate.opsForValue();

        dishDtoPage = (Page<DishDto>) opsForValue.get(key);

        // 缓存命中
        if (dishDtoPage != null){
            return R.success(dishDtoPage);
        }
        // 菜品分页页面
        Page<Dish> dishPage = new Page<>(page,pageSize);
        // 菜品分页交互对象页面
        dishDtoPage = new Page<>();

        // 构造条件过滤器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        // 构建查询条件
        queryWrapper.like(StringUtils.isNotEmpty(name), Dish::getName, name);
        // 分页查询菜品
        dishService.page(dishPage, queryWrapper);

        // 菜品分页记录值
        List<Dish> recordsDish = dishPage.getRecords();

        // 菜品分页交互对象记录值
        List<DishDto> recordsDishDto = recordsDish.stream().map((item) -> {
            // 创建dishDto对象
            DishDto dishDto = new DishDto();

            // 将Dish类型的item属性赋值到dishDto
            BeanUtils.copyProperties(item,dishDto);

            // 获取对象的分类id
            Long categoryId = dishDto.getCategoryId();

            // 根据分类id查分类对象
            Category categoryServiceById = categoryService.getById(categoryId);

            // 从分类对象中取出分类名称
            String categoryServiceByIdName = categoryServiceById.getName();

            // 设置分类名称到dishDto对象
            dishDto.setCategoryName(categoryServiceByIdName);

            // 返回该对象
            return dishDto;
        }).collect(Collectors.toList());

        // 将封装好的记录值赋值给dishDtoPage对象
        dishDtoPage.setRecords(recordsDishDto);
        // 将总条数赋值给dishDtoPage
        dishDtoPage.setTotal(dishPage.getTotal());

        // 将数据缓存进redis
        opsForValue.set(key,dishDtoPage,60,TimeUnit.MINUTES);
        return R.success(dishDtoPage);
    }

    /**
     * @param ids 要删除的菜品id
     * @Description: 批量删除菜品
     * @Author: <a href="https://www.codermast.com/">CoderMast</a>
     */
    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids) {
        dishService.removeBatchByIds(ids);

        // 删除redis缓存
        for (Long id : ids) {
            String key = "dish:" + id;
            redisTemplate.delete(key);
        }

        return R.success("批量删除成功！");
    }

    /**
     * @param id 要获取信息的id值
     * @Description: 根据id查询一个dish对象
     * @Author: CoderMast <a href="https://www.codermast.com/">codermast</a>
     */
    @GetMapping("/{id}")
    public R<DishDto> getOne(@PathVariable String id) {
        DishDto dishDto = null;
        String key = "dish:" + id;

        // 先查询Redis中是否有缓存
        ValueOperations opsForValue = redisTemplate.opsForValue();
        dishDto = (DishDto) opsForValue.get(key);
        // Redis中存在则直接返回
        if (dishDto != null){
            return R.success(dishDto);
        }

        // Redis中不存在，先查数据库，然后添加缓存，再返回
        dishDto = dishService.getByIdWithFlavor(id);

        // 放入缓存，并设置60分钟后失效
        opsForValue.set(key,dishDto,60, TimeUnit.MINUTES);

        return R.success(dishDto);
    }

    /**
     * @Description: 添加菜品
     * @Author: <a href="https://www.codermast.com/">CoderMast</a>
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto) {
        dishService.saveWithFlavor(dishDto);
        ValueOperations opsForValue = redisTemplate.opsForValue();

        String key = "dish:" + dishDto.getId();

        // 将数据缓存进Redis，设置60分过期
        opsForValue.set(key,dishDto,60,TimeUnit.MINUTES);

        return R.success("菜品添加成功");
    }

    /**
     * @Description: 修改菜品信息
     * @param dishDto 菜品的信息
     * @Author: <a href="https://www.codermast.com/">CoderMast</a>
     */
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto){
        boolean ret = dishService.updateById(dishDto);

        String key = "dish:" + dishDto.getId();
        // 更新Redis缓存
        ValueOperations opsForValue = redisTemplate.opsForValue();
        // 放入缓存，设置60分钟失效
        opsForValue.set(key,dishDto,60,TimeUnit.MINUTES);

        return ret? R.success("更新成功"):R.error("更新失败");
    }

    /**
     * @Description: 停售和启售
     * @param status : 状态码，0为停售，1为启售
     * @param ids 操作菜品的id列表
     * @Author: <a href="https://www.codermast.com/">CoderMast</a>
     */
    @PostMapping("/status/{status}")
    public R<String> status(@PathVariable Integer status,@RequestParam List<Long> ids){
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.in(ids != null,Dish::getId,ids);

        List<Dish> list = dishService.list(queryWrapper);

        for (Dish dish : list) {
            if (dish != null){
                dish.setStatus(status);
                dishService.updateById(dish);
            }
        }
        return R.success(status == 1? "启售成功" : "停售成功");
    }

    /**
     * @Description: 根据分类id查询其下的菜品
     * @Author: <a href="https://www.codermast.com/">CoderMast</a>
     */
    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish){
        List<DishDto> dishDtoList = null;

        // 先从redis中获取缓存数据
        ValueOperations opsForValue = redisTemplate.opsForValue();

        String key = "dish:" + dish.getCategoryId();
        // 这里缓存的key是dish的分类id，Value为其序列化的值
        dishDtoList = (List<DishDto>) opsForValue.get(key);
        // redis中存在数据，则直接返回，无需查询数据库
        if (dishDtoList != null){
            log.info("Redis中有缓存，获取的是缓存数据");
            return R.success(dishDtoList);
        }

        //构造查询条件
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(dish.getCategoryId() != null ,Dish::getCategoryId,dish.getCategoryId());
        //添加条件，查询状态为1（起售状态）的菜品
        queryWrapper.eq(Dish::getStatus,1);

        //添加排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        List<Dish> list = dishService.list(queryWrapper);

        dishDtoList = list.stream().map((item) -> {
            DishDto dishDto = new DishDto();

            BeanUtils.copyProperties(item,dishDto);

            Long categoryId = item.getCategoryId();//分类id
            //根据id查询分类对象
            Category category = categoryService.getById(categoryId);

            if(category != null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }

            //当前菜品的id
            Long dishId = item.getId();
            LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(DishFlavor::getDishId,dishId);
            //SQL:select * from dish_flavor where dish_id = ?
            List<DishFlavor> dishFlavorList = dishFlavorService.list(lambdaQueryWrapper);
            dishDto.setFlavors(dishFlavorList);
            return dishDto;
        }).collect(Collectors.toList());

        //redis中不存在数据，则先查询数据库，然后将数据缓存进redis中，在返回数据

        // 将数据存入缓存，设置60分钟失效
        opsForValue.set(key,dishDtoList,60,TimeUnit.MINUTES);
        log.info("Redis中没有缓存，查询了数据库");
        return R.success(dishDtoList);
    }
}
