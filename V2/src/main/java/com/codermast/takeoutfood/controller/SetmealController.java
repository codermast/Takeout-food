package com.codermast.takeoutfood.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codermast.takeoutfood.common.BaseContext;
import com.codermast.takeoutfood.common.R;
import com.codermast.takeoutfood.dto.SetMealDto;
import com.codermast.takeoutfood.entity.Category;
import com.codermast.takeoutfood.entity.SetMeal;
import com.codermast.takeoutfood.service.CategoryService;
import com.codermast.takeoutfood.service.SetMealService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Description: 套餐管理控制器
 * @author: CoderMast
 * @date: 2022/11/27
 * @Blog: <a href="https://www.codermast.com/">codermast</a>
 */
@RestController
@RequestMapping("/setmeal")
public class SetmealController {
    @Autowired
    SetMealService setMealService;

    @Autowired
    CategoryService categoryService;

    @Autowired
    RedisTemplate redisTemplate;
    /**
     * @Description: 分页获取套餐信息
     * @param page 页码
     * @param pageSize 页面大小
     * @param name 关键词
     * @Author: <a href="https://www.codermast.com/">CoderMast</a>
     */
    @GetMapping("/page")
    public R<Page<SetMealDto>> page(int page, int pageSize, String name){
        String key = "category:page_" + page + ":pageSize_" + pageSize + ":name_" + name;
        // 查询缓存
        ValueOperations opsForValue = redisTemplate.opsForValue();
        Page<SetMealDto> setMealDtoPage = null;
        setMealDtoPage = (Page<SetMealDto>) opsForValue.get(key);

        // 缓存命中
        if (setMealDtoPage != null){
            return R.success(setMealDtoPage);
        }

        Page<SetMeal> setMealPage = new Page<>(page,pageSize);
        setMealDtoPage = new Page<>();


        LambdaQueryWrapper<SetMeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.isNotEmpty(name),SetMeal::getName,name);

        setMealService.page(setMealPage,queryWrapper);

        List<SetMeal> setMealPageRecords = setMealPage.getRecords();

        List<SetMealDto> setMealDtoPageRecords = setMealPageRecords.stream().map((item) -> {
            SetMealDto setMealDto = new SetMealDto();

            BeanUtils.copyProperties(item, setMealDto);
            Long setMealDtoCategoryId = setMealDto.getCategoryId();

            Category category = categoryService.getById(setMealDtoCategoryId);
            setMealDto.setCategoryName(category.getName());

            return setMealDto;
        }).collect(Collectors.toList());

        setMealDtoPage.setTotal(setMealPage.getTotal());
        setMealDtoPage.setRecords(setMealDtoPageRecords);

        // 存入缓存
        opsForValue.set(key,setMealDtoPage,60, TimeUnit.MINUTES);
        return R.success(setMealDtoPage);
    }

    /**
     * @Description: 根据套餐id获取套餐信息
     * @param id 套餐id
     * @Author: <a href="https://www.codermast.com/">CoderMast</a>
     */
    @GetMapping("/{id}")
    public R<SetMealDto> getByIdWithSetMealDto(@PathVariable String id){
        // 查询缓存
        ValueOperations opsForValue = redisTemplate.opsForValue();
        SetMealDto byIdWithSetMealDto = null;
        String key = "setMealDto:id:" + id;
        byIdWithSetMealDto = (SetMealDto) opsForValue.get(key);

        // 缓存命中
        if (byIdWithSetMealDto != null){
            return R.success(byIdWithSetMealDto);
        }

        byIdWithSetMealDto = setMealService.getByIdWithSetMealDto(id);

        // 存入缓存
        opsForValue.set(key,byIdWithSetMealDto,60,TimeUnit.MINUTES);
        return R.success(byIdWithSetMealDto);
    }

    /**
     * @Description: 创建套餐
     * @param setMealDto 套餐对象
     * @Author: <a href="https://www.codermast.com/">CoderMast</a>
     */
    @PostMapping
    public R<String> save(@RequestBody SetMealDto setMealDto){
        setMealService.saveByIdWithSetMealDto(setMealDto);

        // 存入缓存
        String key = "setMealDto:id:" + setMealDto.getId();
        redisTemplate.opsForValue().set(key,setMealDto,60,TimeUnit.MINUTES);
        return R.success("创建成功");
    }

    /**
     * @Description: 更新一个套餐
     * @param setMealDto 套餐传输对象实体
     * @Author: <a href="https://www.codermast.com/">CoderMast</a>
     */
    @PutMapping
    public R<String> updateWithDish(@RequestBody SetMealDto setMealDto, HttpServletRequest request){
        boolean ret = setMealService.updateWithDish(setMealDto);

        // 存入缓存
        String key = "setMealDto:id:" + setMealDto.getId();
        redisTemplate.opsForValue().set(key,setMealDto,60,TimeUnit.MINUTES);

        return ret?R.success("修改成功") : R.error("修改失败");
    }

    /**
     * @Description: 根据id批量删除套餐
     * @param ids id列表
     * @Author: <a href="https://www.codermast.com/">CoderMast</a>
     */
    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids){
        boolean ret = setMealService.removeBatchByIds(ids);

        //删除缓存
        for (Long id : ids) {
            String key = "setMealDto:id:" + id;
            redisTemplate.delete(key);
        }
        return ret?R.success("删除成功") : R.error("删除失败");
    }

    /**
     * @Description: 停售和启售
     * @param status : 状态码，0为停售，1为启售
     * @param ids 操作菜品的id列表
     * @Author: <a href="https://www.codermast.com/">CoderMast</a>
     */
    @PostMapping("/status/{status}")
    public R<String> status(@PathVariable Integer status,@RequestParam List<Long> ids,HttpServletRequest request){
        Long id = (Long) request.getSession().getAttribute("employee");
        BaseContext.setCurrentId(id);

        LambdaQueryWrapper<SetMeal> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.in(ids != null,SetMeal::getId,ids);

        List<SetMeal> list = setMealService.list(queryWrapper);

        for (SetMeal setMeal : list) {
            if (setMeal != null){
                setMeal.setStatus(status);
                setMealService.updateById(setMeal);
            }
        }
        return R.success(status == 1? "启售成功" : "停售成功");
    }

    /**
     * @Description: 根据分类id查询其下的菜品
     * @param categoryId 分类id
     * @Author: <a href="https://www.codermast.com/">CoderMast</a>
     */
    @GetMapping("/list")
    public R<List<SetMeal>> getListByCategoryIdWithDish(String categoryId,Integer status){
        List<SetMeal> listByCategoryIdWithSetMeal = null;

        // 查询缓存
        ValueOperations opsForValue = redisTemplate.opsForValue();

        String key = "setMeal:category_" + categoryId + ":status_" + status;
        listByCategoryIdWithSetMeal = (List<SetMeal>) opsForValue.get(key);
        // 缓存击中
        if (listByCategoryIdWithSetMeal != null){
            return R.success(listByCategoryIdWithSetMeal);
        }

        listByCategoryIdWithSetMeal = setMealService.getListByCategoryIdWithSetMeal(categoryId, status);
        // 存入缓存
        opsForValue.set(key,listByCategoryIdWithSetMeal,60,TimeUnit.MINUTES);
        return R.success(listByCategoryIdWithSetMeal);
    }
}
