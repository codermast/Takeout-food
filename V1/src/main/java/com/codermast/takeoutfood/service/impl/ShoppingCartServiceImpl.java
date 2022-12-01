package com.codermast.takeoutfood.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codermast.takeoutfood.service.ShoppingCartService;
import com.codermast.takeoutfood.entity.ShoppingCart;
import com.codermast.takeoutfood.mapper.ShoppingCartMapper;
import org.springframework.stereotype.Service;

/**
 * @Description: 购物车服务实现类
 * @author: CoderMast
 * @date: 2022/11/30
 * @Blog: <a href="https://www.codermast.com/">codermast</a>
 */
@Service
public class ShoppingCartServiceImpl extends ServiceImpl<ShoppingCartMapper,ShoppingCart> implements ShoppingCartService {
}
