package com.codermast.takeoutfood.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.codermast.takeoutfood.common.BaseContext;
import com.codermast.takeoutfood.common.R;
import com.codermast.takeoutfood.entity.AddressBook;
import com.codermast.takeoutfood.service.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Description: 地址控制器
 * @author: CoderMast
 * @date: 2022/11/30
 * @Blog: <a href="https://www.codermast.com/">codermast</a>
 */
@Slf4j
@RestController
@RequestMapping("/addressBook")
public class AddressBookController {
    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * @Description: 获取地址列表
     * @Author: <a href="https://www.codermast.com/">CoderMast</a>
     */
    @GetMapping("/list")
    public R<List<AddressBook>> list() {
        // 从线程中获取用户id
        Long userId = BaseContext.getCurrentId();
        // 构建返回对象
        List<AddressBook> list = null;
        String key = "addressBook:list:" + userId;
        // 查询redis缓存
        ValueOperations opsForValue = redisTemplate.opsForValue();
        list = (List<AddressBook>) opsForValue.get(key);

        // 判断缓存是否命中
        if (list != null) {
            // 缓存命中
            return R.success(list);
        }

        // 构建查询器
        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        // 添加查询条件
        queryWrapper.eq(AddressBook::getUserId, userId);

        list = addressBookService.list(queryWrapper);
        // 将数据添加进缓存
        opsForValue.set(key,list,60, TimeUnit.MINUTES);
        // 返回数据
        return R.success(list);
    }

    /**
     * @param addressBook 地址封装对象
     * @Description: 添加地址信息
     * @Author: <a href="https://www.codermast.com/">CoderMast</a>
     */
    @PostMapping
    public R<String> save(@RequestBody AddressBook addressBook) {
        // 从线程中获取当前登录的用户id
        Long userId = BaseContext.getCurrentId();
        // 给地址赋值用户id
        addressBook.setUserId(userId);
        // 添加地址
        addressBookService.save(addressBook);

        // 添加进缓存
        String key = "addressBook:one:" + addressBook.getId();
        redisTemplate.opsForValue().set(key,addressBook,60,TimeUnit.MINUTES);
        return R.success("保存成功");
    }

    /**
     * @param addressBook 地址封装对象
     * @Description: 设置默认地址
     * @Author: <a href="https://www.codermast.com/">CoderMast</a>
     */
    @PutMapping("/default")
    public R<String> update(@RequestBody AddressBook addressBook) {
        Long userId = BaseContext.getCurrentId();

        // 先将该用户名下的所有地址全改成非默认
        LambdaUpdateWrapper<AddressBook> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(AddressBook::getUserId, userId);
        updateWrapper.set(AddressBook::getIsDefault, 0);
        addressBookService.update(updateWrapper);

        // 指定需要改为默认的地址
        addressBook.setIsDefault(1);
        addressBookService.updateById(addressBook);
        return R.success("设置默认地址成功！");
    }

    /**
     * @param id 地址id
     * @Description: 根据地址分类查询地址信息
     * @Author: <a href="https://www.codermast.com/">CoderMast</a>
     */
    @GetMapping("/{id}")
    public R<AddressBook> getAddressBookById(@PathVariable String id) {
        AddressBook addressBook = null;

        String key = "addressBook:one:" + id;

        // 查询缓存
        ValueOperations opsForValue = redisTemplate.opsForValue();
        addressBook = (AddressBook) opsForValue.get(key);

        // 判断缓存是否命中
        // 命中，直接返回
        if (addressBook != null){
            return R.success(addressBook);
        }
        // 未命中，查库、加缓存、返回
        addressBook = addressBookService.getById(id);
        opsForValue.set(key,addressBook,60,TimeUnit.MINUTES);
        return R.success(addressBook);
    }

    /**
     * @param addressBook 地址封装对象
     * @Description: 更新地址信息
     * @Author: <a href="https://www.codermast.com/">CoderMast</a>
     */
    @PutMapping
    public R<String> updateAddressBook(@RequestBody AddressBook addressBook) {
        addressBookService.updateById(addressBook);
        String key = "addressBook:one:" + addressBook.getId();
        // 更新缓存
        ValueOperations opsForValue = redisTemplate.opsForValue();
        opsForValue.set(key,addressBook,60,TimeUnit.MINUTES);
        return R.success("更新成功");
    }

    /**
     * @param ids 地址id列表
     * @Description: 删除地址
     * @Author: <a href="https://www.codermast.com/">CoderMast</a>
     */
    @DeleteMapping
    public R<String> delete(@RequestParam List<String> ids) {
        addressBookService.removeBatchByIds(ids);

        // 删除缓存
        for (String id : ids) {
            String key = "addressBook:one:" + id;
            redisTemplate.delete(key);
        }
        return R.success("删除成功");
    }

    /**
     * @Description: 获取默认地址
     * @Author: <a href="https://www.codermast.com/">CoderMast</a>
     */
    @GetMapping("/default")
    public R<AddressBook> getDefault() {
        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(AddressBook::getUserId, BaseContext.getCurrentId());
        queryWrapper.eq(AddressBook::getIsDefault, 1);

        AddressBook addressBook = addressBookService.getOne(queryWrapper);
        return R.success(addressBook);
    }
}
