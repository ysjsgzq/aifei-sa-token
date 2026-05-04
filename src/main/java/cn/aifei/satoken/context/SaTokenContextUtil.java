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

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.context.model.SaResponse;
import cn.dev33.satoken.context.model.SaStorage;

/**
 * Sa-Token 上下文绑定工具。
 *
 * <p>用于在请求进入时将当前请求、响应与存储实现绑定到 Sa-Token 上下文，
 * 并在请求完成后统一清理，避免业务代码直接依赖底层上下文实现细节。</p>
 */
public class SaTokenContextUtil {

    private SaTokenContextUtil() {}

    /**
     * 将当前请求相关对象写入 Sa-Token 上下文。
     *
     * @param request 当前请求包装
     * @param response 当前响应包装
     * @param storage 当前请求级存储包装
     */
    public static void setContext(SaRequest request, SaResponse response, SaStorage storage) {
        SaManager.getSaTokenContext().setContext(request, response, storage);
    }

    /**
     * 清理当前线程上绑定的 Sa-Token 上下文。
     */
    public static void clearContext() {
        SaManager.getSaTokenContext().clearContext();
    }
}
