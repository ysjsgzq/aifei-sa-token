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
import cn.dev33.satoken.exception.SaTokenException;
import cn.dev33.satoken.exception.StopMatchException;
import cn.dev33.satoken.filter.SaFilter;
import cn.dev33.satoken.filter.SaFilterAuthStrategy;
import cn.dev33.satoken.filter.SaFilterErrorStrategy;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.strategy.SaAnnotationStrategy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Aifei 版本的 Sa-Token 综合拦截器。
 *
 * <p>该拦截器同时具备以下能力：</p>
 * <p>1. 按包含/排除路径规则决定当前请求是否参与鉴权。</p>
 * <p>2. 在路径命中后执行自定义鉴权逻辑。</p>
 * <p>3. 可选执行方法注解鉴权，使路径规则与注解规则协同工作。</p>
 * <p>4. 复用 Sa-Token 的 beforeAuth、error 等过滤器扩展语义。</p>
 *
 * <p>适合直接作为 Aifei AOP 拦截器挂载在控制层入口。</p>
 */
public class SaTokenInterceptor implements Interceptor, SaFilter {

    /**
     * 是否在路径匹配通过后继续执行方法注解鉴权。
     */
    public boolean isAnnotation = true;

    /**
     * 需要参与鉴权的路径列表。
     */
    protected List<String> includeList = new ArrayList<>();
    /**
     * 需要从鉴权中排除的路径列表。
     */
    protected List<String> excludeList = new ArrayList<>();

    /**
     * 核心鉴权逻辑，在路径命中且注解允许继续时执行。
     */
    protected SaFilterAuthStrategy auth = r -> {};
    /**
     * 鉴权失败时的错误处理策略。
     */
    protected SaFilterErrorStrategy error = SaTokenErrorSupport.DEFAULT_ERROR;
    /**
     * 真正进入路径匹配前执行的前置逻辑。
     */
    protected SaFilterAuthStrategy beforeAuth = r -> {};

    /**
     * 执行完整的 Sa-Token 拦截流程。
     *
     * <p>顺序如下：</p>
     * <p>1. 先执行 {@link #beforeAuth} 前置逻辑。</p>
     * <p>2. 按 include/exclude 规则执行路径匹配。</p>
     * <p>3. 命中后再决定是否执行注解鉴权与自定义鉴权。</p>
     * <p>4. 全部通过后继续调用目标方法。</p>
     *
     * @param inv 当前调用上下文
     * @throws Throwable 未被拦截器转换处理的底层异常
     */
    @Override
    public void intercept(Invocation inv) throws Throwable {
        try {
            if (beforeAuth != null) {
                beforeAuth.run(inv);
            }

            SaRouter.match(includeList).notMatch(excludeList).check(r -> {
                if (checkAnnotation(inv)) {
                    auth.run(inv);
                }
            });

            inv.invoke();
        } catch (StopMatchException ignored) {
            inv.invoke();
        } catch (Throwable e) {
            if (!SaTokenErrorSupport.handleThrowable(inv, e, error)) {
                throw e;
            }
        }
    }

    /**
     * 检查当前调用是否允许继续执行自定义鉴权逻辑。
     *
     * <p>当启用注解鉴权时，会调用 Sa-Token 注解策略校验当前方法。
     * 如果抛出 {@link StopMatchException}，表示注解层要求停止继续匹配，
     * 此时返回 {@code false}，从而跳过后续 {@link #auth} 逻辑。</p>
     *
     * @param inv 当前方法调用
     * @return 是否允许继续执行自定义鉴权逻辑
     */
    protected boolean checkAnnotation(Invocation inv) {
        if (isAnnotation) {
            try {
                SaAnnotationStrategy.instance.checkMethodAnnotation.accept(inv.getMethod());
            } catch (StopMatchException ignored) {
                return false;
            }
        }
        return true;
    }

    /**
     * 追加需要纳入鉴权的路径。
     *
     * @param paths 路径规则数组
     * @return 当前拦截器实例
     */
    @Override
    public SaTokenInterceptor addInclude(String... paths) {
        includeList.addAll(Arrays.asList(paths));
        return this;
    }

    /**
     * 追加需要排除鉴权的路径。
     *
     * @param paths 路径规则数组
     * @return 当前拦截器实例
     */
    @Override
    public SaTokenInterceptor addExclude(String... paths) {
        excludeList.addAll(Arrays.asList(paths));
        return this;
    }

    /**
     * 直接替换包含路径列表。
     *
     * @param pathList 新的包含路径列表
     * @return 当前拦截器实例
     */
    @Override
    public SaTokenInterceptor setIncludeList(List<String> pathList) {
        includeList = pathList;
        return this;
    }

    /**
     * 直接替换排除路径列表。
     *
     * @param pathList 新的排除路径列表
     * @return 当前拦截器实例
     */
    @Override
    public SaTokenInterceptor setExcludeList(List<String> pathList) {
        excludeList = pathList;
        return this;
    }

    /**
     * 设置真正执行鉴权判断的逻辑。
     *
     * @param auth 自定义鉴权策略
     * @return 当前拦截器实例
     */
    @Override
    public SaTokenInterceptor setAuth(SaFilterAuthStrategy auth) {
        this.auth = auth;
        return this;
    }

    /**
     * 设置鉴权失败时的错误处理策略。
     *
     * @param error 自定义错误处理器；传入 {@code null} 时恢复默认策略
     * @return 当前拦截器实例
     */
    @Override
    public SaTokenInterceptor setError(SaFilterErrorStrategy error) {
        this.error = error != null ? error : SaTokenErrorSupport.DEFAULT_ERROR;
        return this;
    }

    /**
     * 设置路径匹配前执行的前置逻辑。
     *
     * @param beforeAuth 前置处理策略
     * @return 当前拦截器实例
     */
    @Override
    public SaTokenInterceptor setBeforeAuth(SaFilterAuthStrategy beforeAuth) {
        this.beforeAuth = beforeAuth;
        return this;
    }

    /**
     * 返回当前包含路径列表。
     *
     * @return 包含路径列表
     */
    public List<String> getIncludeList() {
        return includeList;
    }

    /**
     * 返回当前排除路径列表。
     *
     * @return 排除路径列表
     */
    public List<String> getExcludeList() {
        return excludeList;
    }
}
