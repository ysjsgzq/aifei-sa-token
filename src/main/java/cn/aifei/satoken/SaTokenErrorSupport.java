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

package cn.aifei.satoken;

import cn.aifei.aop.Invocation;
import cn.dev33.satoken.exception.BackResultException;
import cn.dev33.satoken.exception.SaTokenException;
import cn.dev33.satoken.exception.StopMatchException;
import cn.dev33.satoken.filter.SaFilterErrorStrategy;

/**
 * Sa-Token 拦截异常处理辅助类。
 *
 * <p>该类将 Aifei 拦截器与 Sa-Token 过滤器语义之间的异常处理逻辑统一起来，
 * 避免注解拦截器、路径拦截器分别维护一套近似重复的分支判断。</p>
 */
class SaTokenErrorSupport {

    /**
     * 默认异常处理策略。
     *
     * <p>如果原始异常已经是 {@link SaTokenException}，则直接抛出；
     * 否则包装为 {@link SaTokenException} 后再抛出。</p>
     */
    static final SaFilterErrorStrategy DEFAULT_ERROR = e -> {
        throw e instanceof SaTokenException ? (SaTokenException) e : new SaTokenException(e);
    };

    private SaTokenErrorSupport() {}

    /**
     * 处理 Sa-Token 相关异常，并按需写回拦截结果。
     *
     * <p>返回 {@code true} 表示异常已经转换为 Aifei 拦截器可消费的返回值，
     * 调用方无需继续抛出；返回 {@code false} 表示该异常不属于此工具负责处理的范围。</p>
     *
     * @param inv 当前 Aifei 调用上下文
     * @param e 捕获到的异常
     * @param error 自定义错误处理策略
     * @return 是否已经完成处理
     */
    static boolean handleThrowable(Invocation inv, Throwable e, SaFilterErrorStrategy error) {
        if (e instanceof StopMatchException) {
            return false;
        }
        if (e instanceof BackResultException) {
            inv.setReturnValue(((BackResultException) e).result);
            return true;
        }
        if (e instanceof SaTokenException) {
            inv.setReturnValue(error.run(e));
            return true;
        }
        return false;
    }
}
