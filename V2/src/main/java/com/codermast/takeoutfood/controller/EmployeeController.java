package com.codermast.takeoutfood.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codermast.takeoutfood.common.BaseContext;
import com.codermast.takeoutfood.common.R;
import com.codermast.takeoutfood.entity.Employee;
import com.codermast.takeoutfood.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * @Description: 用户控制器
 * @author: CoderMast
 * @date: 2022/11/26
 * @Blog: <a href="https://www.codermast.com/">codermast</a>
 */
@Slf4j
@RestController
@RequestMapping("/employee")
public class EmployeeController {
    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * @param employee 登录信息封装对象
     * @Description: 员工登录
     * @Author: CoderMast <a href="https://www.codermast.com/">...</a>
     */
    @PostMapping("/login")
    public R<Employee> login(HttpServletRequest request, @RequestBody Employee employee) {
        //1、将页面提交的密码password进行md5加密处理
        String password = employee.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        //2、根据页面提交的用户名username查询数据库
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Employee::getUsername, employee.getUsername());
        Employee emp = employeeService.getOne(queryWrapper);

        //3、如果没有查询到则返回登录失败结果
        if (emp == null) {
            return R.error("登录失败");
        }

        //4、密码比对，如果不一致则返回登录失败结果
        if (!emp.getPassword().equals(password)) {
            return R.error("登录失败");
        }

        //5、查看员工状态，如果为已禁用状态，则返回员工已禁用结果
        if (emp.getStatus() == 0) {
            return R.error("账号已禁用");
        }

        //6、登录成功，将员工id存入redis并返回登录成功结果
        ValueOperations opsForValue = redisTemplate.opsForValue();
        String key ="employee:login:" + emp.getId();
        opsForValue.set(key,emp,30, TimeUnit.MINUTES);

        BaseContext.setCurrentId(emp.getId());

        return R.success(emp);
    }

    /**
     * @Description: 员工退出
     * @Author: CoderMast <a href="https://www.codermast.com/">...</a>
     */
    @PostMapping("/logout")
    public R<String> logout() {
        String key ="employee:login:" + BaseContext.getCurrentId();

        redisTemplate.delete(key);
        return R.success("退出成功");
    }

    /**
     * @param employee 将请求内容封装为Employee对象接收
     * @Description: 新增员工
     * @Author: CoderMast <a href="https://www.codermast.com/">...</a>
     */
    @PostMapping
    public R<String> save(@RequestBody Employee employee) {
        log.info("新增员工....{}", employee);

        // 为员工设置默认的登录密码，为123456
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));
        // 设置创建和登录时间
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());

        // 获取当前登录用户信息
        Long curUserId = BaseContext.getCurrentId();

        // 设置创建人和修改人的id
        employee.setCreateUser(curUserId);
        employee.setUpdateUser(curUserId);

        employeeService.save(employee);

        // 缓存进redis中
        String key = "employee:cached:" + employee.getId();
        redisTemplate.opsForValue().set(key,employee,30,TimeUnit.MINUTES);
        return R.success("新增员工成功");
    }

    /**
     * @Description: 查询员工信息
     * @param page  页码
     * @param pageSize 页面尺寸
     * @param name 员工姓名
     * @Author: CoderMast <a href="https://www.codermast.com/">...</a>
     */
    @GetMapping("/page")
    public R<Page<Employee>> page(int page, int pageSize, String name) {

        Page<Employee> pageInfo = null;
        log.info(page + ":" + pageSize + ":" + name);

        String key = "page_" + page + ":pageSize_" + pageSize + ":name_" + name;
        ValueOperations opsForValue = redisTemplate.opsForValue();
        pageInfo = (Page<Employee>) opsForValue.get(key);

        if (pageInfo != null){
            return R.success(pageInfo);
        }

        // 构造分页构造器
        pageInfo = new Page<>(page,pageSize);

        // 构造条件过滤器
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();

        // 添加查询条件
        queryWrapper.like(StringUtils.isNotEmpty(name),Employee::getName,name);

        // 添加排序条件
        queryWrapper.orderByDesc(Employee::getUpdateTime);

        employeeService.page(pageInfo,queryWrapper);

        // 将数据缓存进redis
        opsForValue.set(key,pageInfo,30,TimeUnit.MINUTES);
        return R.success(pageInfo);
    }

    /**
     * @Description: 根据id修改员工信息
     * @param employee 封装对象信息,前端仅有id和status正确传送
     * @Author: CoderMast <a href="https://www.codermast.com/">...</a>
     */
    @PutMapping
    public R<String> update(@RequestBody Employee employee){
        log.info(employee.toString());
        employeeService.updateById(employee);

        // 缓存进redis中
        String key = "employee:cached:" + employee.getId();
        redisTemplate.opsForValue().set(key,employee,30,TimeUnit.MINUTES);
        return R.success("员工信息修改成功");
    }
    /**
     * @Description: 根据id查员工信息
     * @param id 用户id
     * @Author: CoderMast <a href="https://www.codermast.com/">...</a>
     */
    @GetMapping("/{id}")
    public R<Employee> getById(@PathVariable Long id){
        Employee employee = null;

        String key = "employee:cached:" + id;
        ValueOperations opsForValue = redisTemplate.opsForValue();
        employee = (Employee) opsForValue.get(key);
        // 缓存命中
        if (employee != null){
            return R.success(employee);
        }

        employee = employeeService.getById(id);
        if (employee != null) {
            // 缓存未命中,查询数据库，并缓存
            opsForValue.set(key,employee,30,TimeUnit.MINUTES);
            return R.success(employee);
        }
        return R.error("没有该用户！");
    }
}
