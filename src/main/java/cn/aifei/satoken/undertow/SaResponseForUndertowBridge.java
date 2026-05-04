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

import cn.aifei.satoken.context.UndertowResponseUtil;
import cn.dev33.satoken.context.model.SaCookie;
import cn.dev33.satoken.context.model.SaResponse;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

/**
 * 基于 Undertow 的 Sa-Token 响应桥接实现。
 *
 * <p>该类负责把 Sa-Token 抽象层中的响应写操作映射到当前线程绑定的
 * {@link HttpServerExchange} 上，包括状态码、响应头、Cookie 和重定向。</p>
 */
class SaResponseForUndertowBridge implements SaResponse {

    /**
     * 返回底层响应源对象。
     *
     * @return 当前线程绑定的 {@link HttpServerExchange}
     */
    @Override
    public Object getSource() {
        return SaTokenUndertowContextHolder.getExchange();
    }

    /**
     * 向当前响应写入 Cookie。
     *
     * @param cookie Sa-Token Cookie 抽象
     */
    @Override
    public void addCookie(SaCookie cookie) {
        HttpServerExchange exchange = SaTokenUndertowContextHolder.getExchange();
        if (exchange != null) {
            UndertowResponseUtil.addCookie(exchange, cookie);
        }
    }

    /**
     * 设置响应状态码。
     *
     * @param sc HTTP 状态码
     * @return 当前响应对象
     */
    @Override
    public SaResponse setStatus(int sc) {
        HttpServerExchange exchange = SaTokenUndertowContextHolder.getExchange();
        if (exchange != null) {
            exchange.setStatusCode(sc);
        }
        return this;
    }

    /**
     * 覆盖写入响应头。
     *
     * @param name 响应头名称
     * @param value 响应头值
     * @return 当前响应对象
     */
    @Override
    public SaResponse setHeader(String name, String value) {
        HttpServerExchange exchange = SaTokenUndertowContextHolder.getExchange();
        if (exchange != null) {
            exchange.getResponseHeaders().put(HttpString.tryFromString(name), value);
        }
        return this;
    }

    /**
     * 追加写入响应头。
     *
     * @param name 响应头名称
     * @param value 响应头值
     * @return 当前响应对象
     */
    @Override
    public SaResponse addHeader(String name, String value) {
        HttpServerExchange exchange = SaTokenUndertowContextHolder.getExchange();
        if (exchange != null) {
            exchange.getResponseHeaders().add(HttpString.tryFromString(name), value);
        }
        return this;
    }

    /**
     * 发送重定向响应。
     *
     * @param url 目标地址
     * @return 与 Sa-Token 接口兼容的返回值
     */
    @Override
    public Object redirect(String url) {
        HttpServerExchange exchange = SaTokenUndertowContextHolder.getExchange();
        return exchange != null ? UndertowResponseUtil.redirect(exchange, url) : null;
    }
}
