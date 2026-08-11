package com.hmdp.controller;


import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.entity.UserInfo;
import com.hmdp.service.IUserInfoService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@RestController
@RequestMapping("/user")
@Api(tags = "用户相关接口")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;


    @GetMapping("/show")
    public Result show(){
        return userService.show();
    }

    /**
     * 发送手机验证码
     */
    @PostMapping("/code")
    @ApiOperation("发送验证码")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        return userService.sendCode(phone, session);
    }

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @PostMapping("/login")
    @ApiOperation("用户登录")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session){
        return userService.login(loginForm, session);
    }

    @PostMapping("/setPassword")
    public Result setPassword(@RequestParam String password,@RequestParam String phone,@RequestParam String code){
        return userService.setPassword(phone,password,code);
    }

    /**
     * 登出功能
     * @return 无
     */
    @PostMapping("/logout")
    @ApiOperation("用户登出")
    public Result logout(HttpServletRequest request){
        return userService.logout(request);
    }

    @GetMapping("/me")
    @ApiOperation("查看当前用户界面")
    public Result me(){
        User user = userService.getById(UserHolder.getUser().getId());
        UserInfo userInfo = userInfoService.getById(UserHolder.getUser().getId());
        Map<String,Object> map = new HashMap<>();
        map.put("user",user);
        map.put("userInfo",userInfo);
        return Result.ok(map);
    }

    @GetMapping("/{id}")
    @ApiOperation("根据id查询用户信息")
    public Result getUserInfo(@PathVariable("id") Long id){
        if(UserHolder.getUser().getId().equals(id)){
            return Result.fail("");
        }
        User user = userService.getById(id);
        UserInfo userInfo = userInfoService.getById(UserHolder.getUser().getId());
        Map<String,Object> map = new HashMap<>();
        map.put("user",user);
        map.put("userInfo",userInfo);
        return Result.ok(map);
    }

//    @PostMapping("/sign")
//    @ApiOperation("用户打卡")
//    public Result sign(){
//        return userService.sign();
//    }
//
//    @GetMapping("/sign/count")
//    @ApiOperation("当前连续打卡天数")
//    public Result signCount(){
//        return userService.signCount();
//    }

    @GetMapping("/show/{id}")
    public Result showUserInfo(@PathVariable("id") Long id){
        return userService.getUserInfo(id);
    }


}
