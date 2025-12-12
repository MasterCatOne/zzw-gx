package com.zzw.zzwgx.dto.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 自定义反序列化器：支持单个值或数组
 * 用于处理前端可能传单个值或数组的情况
 */
public class SingleOrListDeserializer extends JsonDeserializer<List<Long>> {
    
    @Override
    public List<Long> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        List<Long> result = new ArrayList<>();
        
        if (node.isArray()) {
            // 如果是数组，遍历所有元素
            for (JsonNode item : node) {
                if (item.isNumber()) {
                    result.add(item.asLong());
                }
            }
        } else if (node.isNumber()) {
            // 如果是单个数字，转换为列表
            result.add(node.asLong());
        } else if (node.isNull()) {
            // 如果是 null，返回空列表
            return result;
        }
        
        return result;
    }
}

