package com.cdc.agent.controller;

import com.cdc.agent.agent.Assistant;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mph/agent")
public class AgentController {

    private final Assistant assistant;

    public AgentController(Assistant assistant){
        this.assistant = assistant;
    }


    /**
     *
     * @param 测试agent chat功能
     * @return message
     * @test
     * curl -X POST http://localhost:8888/mph/agent/chat?userId=user001
          -H "Content-Type: application/json" \
           -d '{
               "message":"My name is User001."
             }'
     */
    @PostMapping("/chat")
    public String chat(@RequestParam String userId , @RequestBody String message){
        return assistant.chat(userId,message);
    }

}

