package com.codermast.takeoutfood.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codermast.takeoutfood.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderDetailMapper extends BaseMapper<OrderDetail> {
}
