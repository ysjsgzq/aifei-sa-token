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

package cn.aifei.satoken.context;

import cn.dev33.satoken.context.model.SaCookie;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.CookieImpl;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

/**
 * Undertow 响应操作辅助工具。
 *
 * <p>负责把 Sa-Token 抽象层中的响应动作翻译为 Undertow 原生操作，
 * 例如写入 Cookie、发送重定向响应等。</p>
 */
public class UndertowResponseUtil {

    private UndertowResponseUtil() {}

    /**
     * 将 Sa-Token 的 Cookie 模型转换为 Undertow Cookie 并写入响应。
     *
     * @param exchange 当前 Undertow 交换对象
     * @param cookie Sa-Token Cookie 定义
     */
    public static void addCookie(HttpServerExchange exchange, SaCookie cookie) {
        CookieImpl target = new CookieImpl(cookie.getName(), cookie.getValue());
        target.setPath(cookie.getPath());
        target.setDomain(cookie.getDomain());
        if (cookie.getMaxAge() >= 0) {
            target.setMaxAge(cookie.getMaxAge());
        }
        target.setHttpOnly(cookie.getHttpOnly());
        target.setSecure(cookie.getSecure());
        if (cookie.getSameSite() != null) {
            target.setSameSiteMode(cookie.getSameSite());
        }
        exchange.setResponseCookie(target);
    }

    /**
     * 发送 302 重定向响应并立即结束当前交换。
     *
     * @param exchange 当前 Undertow 交换对象
     * @param url 目标跳转地址
     * @return 始终返回 {@code null}，用于兼容 Sa-Token 接口签名
     */
    public static Object redirect(HttpServerExchange exchange, String url) {
        exchange.setStatusCode(StatusCodes.FOUND);
        exchange.getResponseHeaders().put(Headers.LOCATION, url);
        exchange.endExchange();
        return null;
    }
}
