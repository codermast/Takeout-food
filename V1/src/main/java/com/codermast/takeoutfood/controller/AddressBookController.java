package com.codermast.takeoutfood.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.codermast.takeoutfood.common.BaseContext;
import com.codermast.takeoutfood.entity.AddressBook;
import com.codermast.takeoutfood.common.R;
import com.codermast.takeoutfood.entity.User;
import com.codermast.takeoutfood.service.AddressBookService;
import com.codermast.takeoutfood.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;

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
    private UserService userService;

    @GetMapping("/list")
    public R<List<AddressBook>> list(HttpSession session){
        Long userId = (Long) session.getAttribute("user");
        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AddressBook::getUserId,userId);
        return R.success(addressBookService.list(queryWrapper));
    }

    @PostMapping
    public R<String> save(@RequestBody AddressBook addressBook,HttpSession session){
        BaseContext.setCurrentId((Long) session.getAttribute("user"));

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone,addressBook.getPhone());
        User user = userService.getOne(queryWrapper);
        addressBook.setUserId(user.getId());
        addressBookService.save(addressBook);

        return R.success("保存成功");
    }

    @PutMapping("/default")
    public R<String> update(@RequestBody AddressBook addressBook){
        Long userId = BaseContext.getCurrentId();

        // 先将该用户名下的所有地址全改成非默认
        LambdaUpdateWrapper<AddressBook> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(AddressBook::getUserId,userId);
        updateWrapper.set(AddressBook::getIsDefault,0);
        addressBookService.update(updateWrapper);

        // 指定需要改为默认的地址
        addressBook.setIsDefault(1);
        addressBookService.updateById(addressBook);
        return  R.success("设置默认地址成功！");
    }

    @GetMapping("/{id}")
    public R<AddressBook> getAddressBookById(@PathVariable String id){
        AddressBook addressBook = addressBookService.getById(id);
        return R.success(addressBook);
    }

    @PutMapping
    public R<String> updateAddressBook(@RequestBody AddressBook addressBook){
        addressBookService.updateById(addressBook);

        return R.success("更新成功");
    }

    @DeleteMapping
    public R<String> delete(@RequestParam List<String> ids){
        addressBookService.removeBatchByIds(ids);
        return R.success("删除成功");
    }

    @GetMapping("/default")
    public R<AddressBook> getDefault(){
        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(AddressBook::getUserId,BaseContext.getCurrentId());
        queryWrapper.eq(AddressBook::getIsDefault,1);

        AddressBook addressBook = addressBookService.getOne(queryWrapper);
        return R.success(addressBook);
    }
 }
