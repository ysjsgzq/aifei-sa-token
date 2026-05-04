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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Ant 风格路径匹配器。
 *
 * <p>该类用于将 Sa-Token 路由规则中常见的通配表达式转换为正则表达式，
 * 再配合缓存机制提升重复匹配场景下的性能。当前实现主要服务于
 * {@link cn.dev33.satoken.router.SaRouter} 在 Aifei 场景下的路径判断。</p>
 */
class PathPatternMatcher {

    /**
     * 已编译表达式缓存，避免相同模式在高频请求下重复构建正则。
     */
    private static final Map<String, PathPatternMatcher> CACHE = new ConcurrentHashMap<>();

    /**
     * 当前匹配器实例内部持有的正则表达式。
     */
    private final Pattern pattern;

    /**
     * 获取指定表达式对应的匹配器实例。
     *
     * @param expr 路径表达式，例如 {@code /user/**}
     * @return 已缓存或新建的匹配器
     */
    static PathPatternMatcher get(String expr) {
        if (expr == null) {
            throw new IllegalArgumentException("expr can not be null.");
        }
        return CACHE.computeIfAbsent(expr, PathPatternMatcher::new);
    }

    /**
     * 根据原始路径表达式创建匹配器。
     *
     * @param expr Ant 风格路径表达式
     */
    private PathPatternMatcher(String expr) {
        this.pattern = Pattern.compile(exprCompile(expr), Pattern.CASE_INSENSITIVE);
    }

    /**
     * 判断给定 URI 是否匹配当前表达式。
     *
     * @param uri 待匹配请求路径
     * @return 匹配返回 {@code true}，否则返回 {@code false}
     */
    boolean matches(String uri) {
        return uri != null && pattern.matcher(uri).find();
    }

    /**
     * 将 Ant 风格路径表达式翻译为完整正则。
     *
     * <p>支持的特性包括：</p>
     * <p>1. {@code *} 匹配单层路径片段。</p>
     * <p>2. {@code **} 匹配任意深度路径。</p>
     * <p>3. {@code {var}} 匹配单层动态路径变量。</p>
     * <p>4. {@code {var_}} 匹配可跨层级的动态路径变量。</p>
     *
     * @param expr 原始路径表达式
     * @return 转换后的正则表达式字符串
     */
    private static String exprCompile(String expr) {
        String compiled = expr.replace(".", "\\.");
        compiled = compiled.replace("$", "\\$");
        compiled = compiled.replace("**", ".[]");
        compiled = compiled.replace("*", "[^/]*");
        if (compiled.contains("{")) {
            if (compiled.indexOf("_}") > 0) {
                compiled = compiled.replaceAll("\\{[^\\}]+?\\_\\}", "(.+?)");
            }
            compiled = compiled.replaceAll("\\{[^\\}]+?\\}", "([^/]+?)");
        }
        if (!compiled.startsWith("/")) {
            compiled = "/" + compiled;
        }
        compiled = compiled.replace(".[]", ".*");
        return "^" + compiled + "$";
    }
}
