package com.cdc.agent.controller;

import com.cdc.agent.agent.Assistant;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
     * curl -X POST http://localhost:8888/mph/agent/chat \
          -H "Content-Type: application/json" \
           -d '{
               "message":"北京仓库在哪里？天气怎么样？"
             }'
     */
    @PostMapping("/chat")
    public String chat(@RequestBody String message){
        return assistant.chat(message);
    }

}

