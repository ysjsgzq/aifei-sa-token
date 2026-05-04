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

import cn.aifei.core.Input;
import io.undertow.server.HttpServerExchange;

/**
 * Undertow 请求线程上下文持有器。
 *
 * <p>用于在一次请求处理过程中，将底层 {@link HttpServerExchange} 以及
 * Aifei 已解析完成的 {@link Input} 绑定到当前线程，供 Sa-Token 请求/响应桥接层读取。</p>
 */
class SaTokenUndertowContextHolder {

    /**
     * 当前线程绑定的 Undertow 交换对象。
     */
    private static final ThreadLocal<HttpServerExchange> EXCHANGE_HOLDER = new ThreadLocal<>();
    /**
     * 当前线程绑定的 Aifei 输入对象。
     */
    private static final ThreadLocal<Input> INPUT_HOLDER = new ThreadLocal<>();

    private SaTokenUndertowContextHolder() {}

    /**
     * 绑定当前请求的 Undertow 交换对象。
     *
     * @param exchange 当前请求的 Exchange
     */
    static void setExchange(HttpServerExchange exchange) {
        EXCHANGE_HOLDER.set(exchange);
    }

    /**
     * 读取当前线程绑定的 Undertow 交换对象。
     *
     * @return 当前 Exchange
     */
    static HttpServerExchange getExchange() {
        return EXCHANGE_HOLDER.get();
    }

    /**
     * 清理当前线程绑定的 Undertow 交换对象。
     */
    static void clearExchange() {
        EXCHANGE_HOLDER.remove();
    }

    /**
     * 绑定当前请求已解析好的 Aifei 输入对象。
     *
     * @param input 当前请求输入
     */
    static void setInput(Input input) {
        INPUT_HOLDER.set(input);
    }

    /**
     * 读取当前线程绑定的 Aifei 输入对象。
     *
     * @return 当前 Input
     */
    static Input getInput() {
        return INPUT_HOLDER.get();
    }

    /**
     * 清理当前线程绑定的 Aifei 输入对象。
     */
    static void clearInput() {
        INPUT_HOLDER.remove();
    }
}
