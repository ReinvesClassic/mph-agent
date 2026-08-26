package com.cdc.agent.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class CalculatorTool {

    @Tool("用于计算两个数的加法")
    public int add(@P("第一个数字") int a, @P("第二个数字") int b) {

        return a + b;
    }

    @Tool("用于计算两个数的减法")
    public int sub(@P("第一个数字") int a, @P("第二个数字") int b) {
        return a - b;
    }
}
