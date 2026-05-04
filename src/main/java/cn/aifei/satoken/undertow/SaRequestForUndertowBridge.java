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
import cn.dev33.satoken.context.model.SaRequest;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 基于 Undertow 的 Sa-Token 请求桥接实现。
 *
 * <p>该类将 Aifei/Undertow 请求上下文适配为 Sa-Token 所需的 {@link SaRequest}。
 * 优先从当前线程绑定的 {@link Input} 中读取参数，以兼容 Aifei 已完成的请求解析；
 * 如果当前线程不存在 Input，则退回到 Undertow 原生查询参数读取方式。</p>
 */
class SaRequestForUndertowBridge implements SaRequest {

    /**
     * 返回底层请求源对象。
     *
     * @return 当前线程绑定的 {@link HttpServerExchange}
     */
    @Override
    public Object getSource() {
        return SaTokenUndertowContextHolder.getExchange();
    }

    /**
     * 获取指定请求参数。
     *
     * <p>优先读取 Aifei 已解析的 {@link Input}，避免重复解析请求体或查询串。</p>
     *
     * @param name 参数名
     * @return 参数值，不存在时返回 {@code null}
     */
    @Override
    public String getParam(String name) {
        Input input = SaTokenUndertowContextHolder.getInput();
        if (input != null) {
            return input.getStr(name);
        }

        HttpServerExchange exchange = SaTokenUndertowContextHolder.getExchange();
        if (exchange == null) {
            return null;
        }
        java.util.Deque<String> values = exchange.getQueryParameters().get(name);
        return values != null && !values.isEmpty() ? values.peekFirst() : null;
    }

    /**
     * 返回所有查询参数名。
     *
     * @return 参数名集合；上下文不存在时返回空集合
     */
    @Override
    public Collection<String> getParamNames() {
        HttpServerExchange exchange = SaTokenUndertowContextHolder.getExchange();
        return exchange != null ? exchange.getQueryParameters().keySet() : java.util.Collections.emptyList();
    }

    /**
     * 返回单值形式的参数映射。
     *
     * <p>当某个参数存在多个值时，仅保留首个值，以符合 Sa-Token 接口的单值语义。</p>
     *
     * @return 参数名到参数值的映射
     */
    @Override
    public Map<String, String> getParamMap() {
        HttpServerExchange exchange = SaTokenUndertowContextHolder.getExchange();
        Map<String, String> result = new LinkedHashMap<>();
        if (exchange == null) {
            return result;
        }
        for (Map.Entry<String, java.util.Deque<String>> entry : exchange.getQueryParameters().entrySet()) {
            java.util.Deque<String> values = entry.getValue();
            if (values != null && !values.isEmpty()) {
                result.put(entry.getKey(), values.peekFirst());
            }
        }
        return result;
    }

    /**
     * 读取指定请求头。
     *
     * @param name 请求头名称
     * @return 请求头值
     */
    @Override
    public String getHeader(String name) {
        HttpServerExchange exchange = SaTokenUndertowContextHolder.getExchange();
        return exchange != null ? exchange.getRequestHeaders().getFirst(name) : null;
    }

    /**
     * 返回指定 Cookie 的值。
     *
     * @param name Cookie 名称
     * @return Cookie 值
     */
    @Override
    public String getCookieValue(String name) {
        return getCookieFirstValue(name);
    }

    /**
     * 返回指定 Cookie 的首个值。
     *
     * <p>Undertow 对同名 Cookie 通常只暴露一个请求 Cookie，因此此处直接返回该值。</p>
     *
     * @param name Cookie 名称
     * @return Cookie 值
     */
    @Override
    public String getCookieFirstValue(String name) {
        HttpServerExchange exchange = SaTokenUndertowContextHolder.getExchange();
        if (exchange == null) {
            return null;
        }
        Cookie cookie = exchange.getRequestCookie(name);
        return cookie != null ? cookie.getValue() : null;
    }

    /**
     * 返回指定 Cookie 的最后一个值。
     *
     * @param name Cookie 名称
     * @return Cookie 值
     */
    @Override
    public String getCookieLastValue(String name) {
        return getCookieFirstValue(name);
    }

    /**
     * 返回当前请求路径。
     *
     * @return 请求路径
     */
    @Override
    public String getRequestPath() {
        HttpServerExchange exchange = SaTokenUndertowContextHolder.getExchange();
        return exchange != null ? exchange.getRequestPath() : null;
    }

    /**
     * 返回当前请求完整 URL。
     *
     * @return 请求 URL
     */
    @Override
    public String getUrl() {
        HttpServerExchange exchange = SaTokenUndertowContextHolder.getExchange();
        return exchange != null ? exchange.getRequestURL() : null;
    }

    /**
     * 返回 HTTP 请求方法。
     *
     * @return 请求方法名
     */
    @Override
    public String getMethod() {
        HttpServerExchange exchange = SaTokenUndertowContextHolder.getExchange();
        return exchange != null ? exchange.getRequestMethod().toString() : null;
    }

    /**
     * 返回请求主机名。
     *
     * @return 主机名
     */
    @Override
    public String getHost() {
        HttpServerExchange exchange = SaTokenUndertowContextHolder.getExchange();
        return exchange != null ? exchange.getHostName() : null;
    }

    /**
     * Undertow 适配层不支持服务端 forward。
     *
     * @param path 目标路径
     * @return 不返回；总是抛出异常
     */
    @Override
    public Object forward(String path) {
        throw new UnsupportedOperationException("Aifei Undertow does not support request forward.");
    }
}
