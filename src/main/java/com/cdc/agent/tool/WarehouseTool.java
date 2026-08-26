package com.cdc.agent.tool;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class WarehouseTool {

    @Tool("获取当前城市的仓库位置")
    public String getWarehousePosition(String city) {
        //String pos=WarehouseService.getPos(City);
        return "北京市东直门大街36号B座13楼1319室";
    }

}
