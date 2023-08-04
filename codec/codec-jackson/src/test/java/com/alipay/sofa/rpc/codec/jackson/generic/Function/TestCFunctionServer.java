package com.alipay.sofa.rpc.codec.jackson.generic.Function;

import com.alipay.sofa.rpc.codec.jackson.generic.Function.DO.*;

public abstract class TestCFunctionServer extends TestAFunctionServer<FunctionRequest,FunctionResponse>{

    public FunctionResponse apply(FunctionRequest functionRequest) {
        return this.process(functionRequest);
    }

    public abstract FunctionResponse process(FunctionRequest testCFunctionRequest);
}
