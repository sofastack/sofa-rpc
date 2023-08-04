package com.alipay.sofa.rpc.codec.jackson.generic.Function;

import com.alipay.sofa.rpc.codec.jackson.generic.Function.DO.FunctionRequest;
import com.alipay.sofa.rpc.codec.jackson.generic.Function.DO.FunctionResponse;

public class TestDFunctionServer extends TestCFunctionServer {

    @Override
    public FunctionResponse process(FunctionRequest testCFunctionRequest){
        FunctionResponse testCFunctionResponse = new FunctionResponse();
        testCFunctionResponse.setUrl(testCFunctionRequest.getUrl());
        return testCFunctionResponse;
    }

}
