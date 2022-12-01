package com.codermast.takeoutfood.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codermast.takeoutfood.entity.ShoppingCart;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ShoppingCartMapper extends BaseMapper<ShoppingCart> {
}
