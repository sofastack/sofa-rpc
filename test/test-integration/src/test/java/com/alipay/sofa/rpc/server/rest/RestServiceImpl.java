/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alipay.sofa.rpc.server.rest;

import com.alipay.sofa.rpc.context.RpcInternalContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class RestServiceImpl implements RestService {

    /**
     * slf4j Logger for this class
     */
    private final static Logger               LOGGER = LoggerFactory.getLogger(RestServiceImpl.class);

    private final static Map<Integer, String> db     = new ConcurrentHashMap<Integer, String>();

    @Override
    public String add(int code, String name) {
        LOGGER.info("post code is " + code + ", name is " + name);
        db.put(code, name);
        return "create ok !" + code;
    }

    @Override
    public String query(int code) {
        String remote = RpcInternalContext.getContext().getRemoteHostName();
        LOGGER.info("remote:" + remote + " get code:" + code);
        return "hello world !" + db.get(code);
    }

    @Override
    public String query(int code, String name) {
        String remote = RpcInternalContext.getContext().getRemoteHostName();
        LOGGER.info("remote:" + remote + " get code:" + code + ", name is: " + name);
        return "hello world !" + db.get(code);
    }

    public Response update(int code, String name) {
        LOGGER.info("put code is " + code + ", name is " + name);
        db.put(code, name);
        String result = "update ok !" + code;
        return Response.status(200).entity(result).build();
    }

    @Override
    public Response redirect(int code, String name) throws URISyntaxException {

        String result = "redirect ok !" + code;

        return Response.status(302).location(new URI("https://www.alipay.com/")).entity(result).build();
    }

    @Override
    public Response redirectFail(int code, String name) throws URISyntaxException {
        String result = "redirect ok !" + code;

        return Response.status(302).location(new URI("http://cuowuwagnzhan.com/")).entity(result).build();
    }

    @Override
    public String delete(int code) {
        LOGGER.info("delete code:" + code);
        return db.remove(code);
    }

    @Override
    public ExampleObj object(ExampleObj code) {
        code.setName(code.getName() + " server");
        return code;
    }

    @Override
    public List<ExampleObj> objects(List<ExampleObj> codes) {
        for (ExampleObj code : codes) {
            code.setName(code.getName() + " server");
        }
        return codes;
    }

    @Override
    public String get(String code) {
        return "server" + code;
    }

    @Override
    public String error(String code) {
        throw new RuntimeException("abcdefg");
    }

    @Override
    public String post(String code, String body) {
        return "server " + code + body;
    }

    @Override
    public String bindHeader(String headerP) {
        return headerP;
    }

    @Override
    public String bindQuery(String queryP) {
        return queryP;
    }

    @Override
    public String bindForm(String formP) {
        return formP;
    }

    @Override
    public String validationMax(long age) {
        return String.valueOf(age);
    }

    @Override
    public String upLoadFile(String code) {

        return "file ok !" + code;
    }

}