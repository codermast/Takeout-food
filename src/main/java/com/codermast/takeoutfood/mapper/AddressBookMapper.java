package com.codermast.takeoutfood.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codermast.takeoutfood.entity.AddressBook;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AddressBookMapper extends BaseMapper<AddressBook> {
}
