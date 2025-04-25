package com.alipay.sofa.rpc.api.future;

import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.RequestBase;
import com.alipay.sofa.rpc.message.ResponseFuture;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;

import java.util.List;
import java.util.concurrent.CompletableFuture;

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

    @Override
    public ResponseFuture<T> addListeners(List<SofaResponseCallback> callbacks) {
        return delegate.addListeners(callbacks);
    }

    @Override
    public ResponseFuture<T> addListener(SofaResponseCallback callback) {
        return delegate.addListener(callback);
    }

    @Override
    public T get() {
        try {
            // 如果CompletableFuture已经完成，直接返回结果
            if (isDone()) {
                return super.join();
            }
            // 否则调用delegate的get方法
            T result = delegate.get();
            // 完成当前CompletableFuture
            this.complete(result);
            return result;
        } catch (Exception e) {
            this.completeExceptionally(e);
            throw new SofaRpcException(RpcErrorType.SERVER_UNDECLARED_ERROR,
                    "Get response failed, cause: " + e.getMessage(), e);
        }
    }
}
