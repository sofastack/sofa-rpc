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

import javax.annotation.Resource;
import javax.validation.constraints.Min;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URISyntaxException;
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
    public String query(@PathParam("code") int code, @PathParam("name") String name);

    @PUT
    @Path(value = "/hello/{code}/{name}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("code") int code,
                           @PathParam("name") String name);

    @POST
    @Path(value = "/redirect/{code}/{name}")
    public Response redirect(@PathParam("code") int code, @PathParam("name") String name) throws URISyntaxException;

    @POST
    @Path(value = "/redirectFail/{code}/{name}")
    public Response redirectFail(@PathParam("code") int code, @PathParam("name") String name) throws URISyntaxException;

    @DELETE
    @Path(value = "/hello/{code}")
    public String delete(@PathParam("code") int code);

    @POST
    @Path(value = "/object")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes("application/json")
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

    @GET
    @Path(value = "/error/{code}")
    @Produces(MediaType.TEXT_PLAIN)
    public String error(@PathParam("code") String code);

    @POST
    @Path(value = "/post/{code}")
    @Produces(MediaType.TEXT_PLAIN)
    public String post(@PathParam("code") String code, String body);

    @POST
    @Path(value = "/bindHeader")
    @Produces(MediaType.TEXT_PLAIN)
    public String bindHeader(@HeaderParam("headerP") String headerP);

    @GET
    @Path(value = "/bindQuery")
    @Produces(MediaType.TEXT_PLAIN)
    public String bindQuery(@QueryParam("queryP") String queryP);

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path(value = "/bindForm")
    @Produces(MediaType.TEXT_PLAIN)
    public String bindForm(@FormParam("formP") String formP);

    @GET
    @Path(value = "/validationMax/{age}")
    @Produces(MediaType.TEXT_PLAIN)
    public String validationMax(@PathParam("age") @Min(value = 600) long age);

    @POST
    @Path("/file/{code}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String upLoadFile(@PathParam("code") String code);

}
