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
package com.alipay.sofa.rpc.tracer.sofatracer;

import com.alipay.sofa.rpc.core.request.SofaRequest;
import io.grpc.Context;
import io.grpc.Metadata;
import io.opentracing.Span;
import io.opentracing.SpanContext;

public class TracingContextKey {

    public static final String                    KEY_NAME              = "io.opentracing.active-span";
    public static final String                    KEY_CONTEXT_NAME      = "io.opentracing.active-span-context";
    private static final Context.Key<Span>        key                   = Context.key(KEY_NAME);
    private static final Context.Key<SpanContext> keyContext            = Context.key(KEY_CONTEXT_NAME);
    public static final String                    KEY_SOFA_REQUEST_NAME = "io.opentracing.sofa-request";
    private static final Context.Key<SofaRequest> keySofaRequest        = Context.key(KEY_SOFA_REQUEST_NAME);
    private static final Context.Key<Metadata>    keyMetadata           = Context.key("io.opentracing.metadata");

    /**
     * Retrieves the active span.
     *
     * @return the active span for the current request
     */
    public static Span activeSpan() {
        return key.get();
    }

    /**
     * Retrieves the span key.
     *
     * @return the OpenTracing context key
     */
    public static Context.Key<Span> getKey() {
        return key;
    }

    /**
     * Retrieves the span context key.
     *
     * @return the OpenTracing context key for span context
     */
    public static Context.Key<SpanContext> getSpanContextKey() {
        return keyContext;
    }

    public static SpanContext activeSpanContext() {
        return keyContext.get();
    }

    public static Context.Key<SofaRequest> getKeySofaRequest() {
        return keySofaRequest;
    }

    public static Context.Key<Metadata> getKeyMetadata() {
        return keyMetadata;
    }
}
