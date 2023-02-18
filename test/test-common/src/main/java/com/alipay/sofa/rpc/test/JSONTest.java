package com.alipay.sofa.rpc.test;

import com.alipay.sofa.rpc.common.json.JSON;

import java.util.HashMap;
import java.util.Map;

/**
 * @author github.com/gofow
 * @date 2023/02/18 16:55
 **/
public class JSONTest {

    public static void main(String[] args) {
        BeanEntity bean = new JSONTest.BeanEntity();

        Map<String, ? super ValueEntity> map = new HashMap<>();
        map.put("1", new ValueEntity());
        bean.map = map;

        String jsonString = JSON.toJSONString(bean);
        System.out.println(jsonString);

        bean.map.values().forEach(value -> {
            System.out.println(value.getClass().getSimpleName());
        });

        BeanEntity ans = JSON.parseObject(jsonString, BeanEntity.class);
        System.out.println(ans.map.get("1").getClass().getSimpleName());
    }

    static class BeanEntity {
        Map<String, ? super ValueEntity> map;
    }

    static class ValueEntity {}
}
