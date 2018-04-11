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
package com.alipay.sofa.rpc.rest;

import javax.annotation.Resource;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
@Resource
@Path("rest")
@Consumes
public interface RestService {

    @POST
    @Path(value = "/hello/{code}/{name}")
    public String add(@PathParam("code") int code,
                      @PathParam("name") String name);

    @GET
    @Path(value = "/hello/{code}")
    public String query(@PathParam("code") int code);

    @GET
    @Path(value = "/hello2/{code}/{name}")
    public String query(@PathParam("code") int code, String name);

    @PUT
    @Path(value = "/hello/{code}/{name}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("code") int code,
                           @PathParam("name") String name);

    @DELETE
    @Path(value = "/hello/{code}")
    public String delete(@PathParam("code") int code);

    @POST
    @Path(value = "/object")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public ExampleObj object(ExampleObj code);

    @POST
    @Path(value = "/objects")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public List<ExampleObj> objects(List<ExampleObj> code);

    @GET
    @Path(value = "/get/{code}")
    @Produces(MediaType.TEXT_PLAIN)
    public String get(@PathParam("code") String code);

    @POST
    @Path(value = "/post/{code}")
    @Produces(MediaType.TEXT_PLAIN)
    public String post(@PathParam("code") String code, String body);
}
