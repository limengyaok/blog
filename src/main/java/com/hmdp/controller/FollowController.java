package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IFollowService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/follow")
public class FollowController {

    @Autowired
    private IFollowService followService;

    @GetMapping("/or/not/{id}")
    public Result followOrNot(@PathVariable Long id){
        return followService.followOrNot(id);
    }

    @PutMapping("/{id}/{status}")
    public Result followChange(@PathVariable Long id, @PathVariable Boolean status){
        return followService.followChange(id,status);
    }

    @GetMapping("/common/{id}")
    public Result commonFollows(@PathVariable Long id){
        return followService.commonFollows(id);
    }
}
