package com.codermast.takeoutfood.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codermast.takeoutfood.entity.OrderDetail;
import com.codermast.takeoutfood.mapper.OrderDetailMapper;
import com.codermast.takeoutfood.service.OrderDetailService;
import org.springframework.stereotype.Service;

/**
 * @Description: 订单详情服务实现类
 * @author: CoderMast
 * @date: 2022/11/30
 * @Blog: <a href="https://www.codermast.com/">codermast</a>
 */
@Service
public class OrderDetailServiceImpl extends ServiceImpl<OrderDetailMapper, OrderDetail> implements OrderDetailService {
}
