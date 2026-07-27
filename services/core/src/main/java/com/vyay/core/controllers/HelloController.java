package com.vyay.core.controllers;

import com.vyay.core.dto.wrapper.ApiResponse;
import com.vyay.core.services.ApiCounterService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;



@RestController
public class HelloController {

    private final ApiCounterService counterService;

    public HelloController(ApiCounterService counterService) {
        this.counterService = counterService;
    }


    @GetMapping("/hello")
    ApiResponse<String> helloWorld() {
        Long count = counterService.incrementGlobalCount();
        System.out.println("got request....");
        return ApiResponse.success("Hello World From Split Easy: "+count);
    }


}
