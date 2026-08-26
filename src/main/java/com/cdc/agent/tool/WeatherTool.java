package com.cdc.agent.tool;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class WeatherTool {

    @Tool("查询城市当前天气情况，例如晴天、多云、下雨")

    public String weather(String city){return "多云转小雨";}

    @Tool("查询城市当前温度，只返回摄氏温度")
    public String temperature(String city) {return  "30";}
}
