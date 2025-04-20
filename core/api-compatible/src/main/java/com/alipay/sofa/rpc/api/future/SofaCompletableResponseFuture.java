package com.alipay.sofa.rpc.api.future;

import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.RequestBase;
import com.alipay.sofa.rpc.message.ResponseFuture;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class SofaCompletableResponseFuture<T> extends CompletableFuture<T> implements ResponseFuture<T> {

    private final ResponseFuture<T> delegate;

    public SofaCompletableResponseFuture(ResponseFuture<T> delegate) {
        this.delegate = delegate;

        RpcInvokeContext.getContext().setResponseCallback(new SofaResponseCallback<T>() {
            @Override
            public void onAppResponse(Object appResponse, String methodName, RequestBase request) {
                SofaCompletableResponseFuture.this.complete((T) appResponse);
            }

            @Override
            public void onAppException(Throwable throwable, String methodName, RequestBase request) {
                SofaCompletableResponseFuture.this.completeExceptionally(throwable);
            }

            @Override
            public void onSofaException(SofaRpcException sofaException, String methodName, RequestBase request) {
                SofaCompletableResponseFuture.this.completeExceptionally(sofaException);
            }
        });
    }

    public ResponseFuture<T> getDelegate() {
        return delegate;
    }
}
