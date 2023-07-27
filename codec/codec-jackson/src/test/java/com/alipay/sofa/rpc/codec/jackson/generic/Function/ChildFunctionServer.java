package com.alipay.sofa.rpc.codec.jackson.generic.Function;

import com.alipay.sofa.rpc.codec.jackson.generic.Function.DO.FunctionRequest;
import com.alipay.sofa.rpc.codec.jackson.generic.Function.DO.FunctionResponse;
import com.sun.tracing.dtrace.FunctionName;

public class ChildFunctionServer extends TestFunctionServer<FunctionRequest, FunctionResponse>{
    @Override
    public FunctionResponse process(FunctionRequest functionRequest) {
        FunctionResponse functionResponse = new FunctionResponse();
        functionResponse.setUrl(functionRequest.getUrl());
        return functionResponse;
    }
}
