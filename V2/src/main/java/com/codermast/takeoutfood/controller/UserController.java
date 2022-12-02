package com.codermast.takeoutfood.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.codermast.takeoutfood.common.BaseContext;
import com.codermast.takeoutfood.common.R;
import com.codermast.takeoutfood.common.ValidateCodeUtils;
import com.codermast.takeoutfood.entity.User;
import com.codermast.takeoutfood.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @Description: 用户控制器
 * @author: CoderMast
 * @date: 2022/11/29
 * @Blog: <a href="https://www.codermast.com/">codermast</a>
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    StringRedisTemplate redisTemplate;

    /**
     * @Description: 发送短信
     * @param user 手机号封装
     * @Author: <a href="https://www.codermast.com/">CoderMast</a>
     */
    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user){
        // 获取用户的邮箱
        String phone = user.getPhone();

        // 手机号为空
        if (!StringUtils.isNotEmpty(phone)){
            return R.error("手机号为空！");
        }

        // 生成验证码
        String strCode = String.valueOf(ValidateCodeUtils.generateValidateCode(4));
        log.info("Code:" + strCode);

        // 获取键值对操作对象
        ValueOperations<String, String> opsForValue = redisTemplate.opsForValue();

        // 将验证码缓存到redis中,并且设置验证码有效期为5分钟
        opsForValue.set("user:phone:" + phone,strCode,5 ,TimeUnit.MINUTES);

        return R.success("发送成功");
    }

    /**
     * @Description: 用户登录
     * @param map 用户登录手机和验证码
     * @Author: <a href="https://www.codermast.com/">CoderMast</a>
     */
    @PostMapping("/login")
    public R<String> login(@RequestBody Map<String,String> map){
        String phone = map.get("phone");
        String code = map.get("code");

        // 获取ops操作对象
        ValueOperations<String, String> opsForValue = redisTemplate.opsForValue();

        // 从redis中取出验证码
        String attributeCode = opsForValue.get("user:phone:" + phone);

        // 验证码匹配成功时
        if (code.equals(attributeCode)){
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();

            queryWrapper.eq(User::getPhone,phone);
            User user = userService.getOne(queryWrapper);
            // 未注册
            if (user == null){
                // 构建保存实体
                user = new User();
                user.setPhone(phone);// 设置手机号
                user.setStatus(1);   // 设置状态，1为启用
                userService.save(user);
            }
            // 将用户id放在线程中
            BaseContext.setCurrentId(user.getId());

            // 匹配成功意味着登录成功，故直接删除掉该验证码
            redisTemplate.delete(phone);
            return R.success("登录成功");
        }else {
            return R.error("登录失败");
        }
    }

    /**
     * @Description: 用户退出登录
     * @Author: <a href="https://www.codermast.com/">CoderMast</a>
     */
    @PostMapping("/loginout")
    public R<String> loginOut(){
        BaseContext.setCurrentId(null);
        return R.success("退出成功！");
    }
}
