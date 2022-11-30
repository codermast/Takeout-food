package com.codermast.takeoutfood.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.codermast.takeoutfood.dto.SetMealDto;
import com.codermast.takeoutfood.entity.SetMeal;

import java.util.List;

public interface SetMealService extends IService<SetMeal> {

    SetMealDto getByIdWithSetMealDto(String id);

    boolean updateWithDish(SetMealDto setMealDto);

    boolean saveByIdWithSetMealDto(SetMealDto setMealDto);

    List<SetMeal> getListByCategoryIdWithSetMeal(String categoryId, Integer status);
}
