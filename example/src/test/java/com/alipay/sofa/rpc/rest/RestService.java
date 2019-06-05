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

import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import org.jboss.resteasy.spi.HttpRequest;

import javax.annotation.Resource;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
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
    public @ApiResponse(response = ExampleObj.class, message = "response", code = 200)
    ExampleObj object(@ApiParam(value = "参数", required = true) ExampleObj code);

    @GET
    @Path(value = "/api")
    @Produces(MediaType.APPLICATION_JSON)
    public String api();

    @POST
    @Path(value = "/{interfaceName}/{methodName}")
    @Produces(MediaType.APPLICATION_JSON)
    public String invoke(@PathParam("interfaceName") String interfaceName, @PathParam("methodName") String methodName,
                         HttpRequest request);

}
