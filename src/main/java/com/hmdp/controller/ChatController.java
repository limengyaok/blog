package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.entity.Chat;
import com.hmdp.service.IChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private IChatService chatService;

    @GetMapping("/list")
    public Result chatList(){
        return chatService.getList();
    }

    @PostMapping("/create")
    public Result create(@RequestParam Integer userId){
        return chatService.create(userId);
    }
}
