/*
 * Copyright 2011-2035 ysjsgzq
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.aifei.satoken.undertow;

import cn.aifei.core.Handler;
import cn.aifei.core.Input;
import cn.aifei.core.Output;
import cn.aifei.satoken.context.SaTokenContextUtil;
import cn.aifei.server.Dispatcher;
import io.undertow.server.HttpServerExchange;

/**
 * 为 Aifei Undertow Dispatcher 增加 Sa-Token 上下文能力的包装器。
 *
 * <p>该类通过装饰器方式包裹现有 {@link Dispatcher}，在不改动业务 Handler 代码的前提下，
 * 于请求分发前后完成以下工作：</p>
 * <p>1. 绑定当前线程的 Undertow Exchange。</p>
 * <p>2. 装配 Sa-Token 所需的 Request、Response、Storage 上下文对象。</p>
 * <p>3. 在 Handler 执行阶段补充绑定已解析好的 Aifei Input。</p>
 * <p>4. 请求结束后统一清理线程变量，避免上下文泄漏到下一个请求。</p>
 */
public class SaTokenUndertowDispatcher<I extends Input, O extends Output> implements Dispatcher<HttpServerExchange, Void, I, O> {

    /**
     * 被包装的原始 Dispatcher，实现真正的 Undertow 请求分发。
     */
    private final Dispatcher<HttpServerExchange, Void, I, O> delegate;

    /**
     * 创建带 Sa-Token 上下文能力的 Dispatcher 包装器。
     *
     * @param delegate 原始 Dispatcher，不能为空
     */
    public SaTokenUndertowDispatcher(Dispatcher<HttpServerExchange, Void, I, O> delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate can not be null.");
        }
        this.delegate = delegate;
    }

    /**
     * 初始化底层 Dispatcher，并在 Handler 调用期间绑定当前 Input。
     *
     * <p>这样 Sa-Token 请求桥接层既能读取原始 Exchange，也能优先使用
     * Aifei 已完成解析的参数对象。</p>
     *
     * @param handler 业务 Handler
     */
    @Override
    public void init(Handler<I, O> handler) {
        delegate.init(new Handler<I, O>() {
            @Override
            public void handle(String path, I input, O output) throws Throwable {
                try {
                    SaTokenUndertowContextHolder.setInput(input);
                    handler.handle(path, input, output);
                } finally {
                    SaTokenUndertowContextHolder.clearInput();
                }
            }
        });
    }

    /**
     * 分发 Undertow 请求，并在整个分发周期内维护 Sa-Token 上下文。
     *
     * @param exchange 当前 Undertow 请求交换对象
     * @param unused 占位参数，当前实现未使用
     */
    @Override
    public void dispatch(HttpServerExchange exchange, Void unused) {
        try {
            SaTokenUndertowContextHolder.setExchange(exchange);
            SaTokenContextUtil.setContext(
                    new SaRequestForUndertowBridge(),
                    new SaResponseForUndertowBridge(),
                    new SaStorageForUndertowBridge(exchange)
            );
            delegate.dispatch(exchange, unused);
        } finally {
            SaTokenContextUtil.clearContext();
            SaTokenUndertowContextHolder.clearInput();
            SaTokenUndertowContextHolder.clearExchange();
        }
    }
}
