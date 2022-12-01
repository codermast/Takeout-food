package com.codermast.takeoutfood.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codermast.takeoutfood.entity.AddressBook;
import com.codermast.takeoutfood.service.AddressBookService;
import com.codermast.takeoutfood.mapper.AddressBookMapper;
import org.springframework.stereotype.Service;

/**
 * @Description: 地址服务层实现类
 * @author: CoderMast
 * @date: 2022/11/30
 * @Blog: <a href="https://www.codermast.com/">codermast</a>
 */

@Service
public class AddressBookServiceImpl extends ServiceImpl<AddressBookMapper, AddressBook> implements AddressBookService {
}
