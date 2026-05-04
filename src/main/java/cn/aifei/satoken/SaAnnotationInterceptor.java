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

import cn.aifei.aop.Interceptor;
import cn.aifei.aop.Invocation;
import cn.dev33.satoken.exception.StopMatchException;
import cn.dev33.satoken.filter.SaFilterErrorStrategy;
import cn.dev33.satoken.strategy.SaAnnotationStrategy;

/**
 * 基于方法注解的 Sa-Token 鉴权拦截器。
 *
 * <p>该拦截器仅处理诸如 {@code @SaCheckLogin}、{@code @SaCheckRole} 等
 * Sa-Token 注解带来的鉴权逻辑，不负责路径匹配。适合直接挂载在 Aifei AOP 链路中，
 * 让每次方法调用先经过注解鉴权，再进入业务执行。</p>
 */
public class SaAnnotationInterceptor implements Interceptor {

    /**
     * 鉴权失败时的错误处理策略。
     */
    protected SaFilterErrorStrategy error = SaTokenErrorSupport.DEFAULT_ERROR;

    /**
     * 执行注解鉴权。
     *
     * <p>当注解校验抛出 {@link StopMatchException} 时，表示当前方法无需继续走注解匹配失败分支，
     * 拦截器会直接放行业务逻辑；其他 Sa-Token 异常则交由统一错误处理逻辑转换。</p>
     *
     * @param inv 当前方法调用信息
     * @throws Throwable 业务方法或未被转换的底层异常
     */
    @Override
    public void intercept(Invocation inv) throws Throwable {
        try {
            SaAnnotationStrategy.instance.checkMethodAnnotation.accept(inv.getMethod());
            inv.invoke();
        } catch (Throwable e) {
            if (e instanceof StopMatchException) {
                inv.invoke();
                return;
            }
            if (!SaTokenErrorSupport.handleThrowable(inv, e, error)) {
                throw e;
            }
        }
    }

    /**
     * 设置鉴权失败后的错误处理策略。
     *
     * @param error 自定义错误处理逻辑；传入 {@code null} 时恢复默认策略
     * @return 当前拦截器实例，便于链式调用
     */
    public SaAnnotationInterceptor setError(SaFilterErrorStrategy error) {
        this.error = error != null ? error : SaTokenErrorSupport.DEFAULT_ERROR;
        return this;
    }
}
