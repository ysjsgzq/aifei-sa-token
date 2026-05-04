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

import cn.aifei.plugin.Plugin;
import cn.aifei.util.Prop;
import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.plugin.SaTokenPluginHolder;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.fun.strategy.SaRouteMatchFunction;
import cn.dev33.satoken.strategy.SaStrategy;
import java.util.function.Consumer;

/**
 * Aifei 框架下的 Sa-Token 插件入口。
 *
 * <p>该插件负责在应用启动时完成 Sa-Token 所需运行环境的装配，包括：</p>
 * <p>1. 安装当前插件持有的 {@link SaTokenConfig} 配置。</p>
 * <p>2. 将 Sa-Token 上下文切换为基于线程变量的实现，便于在 Undertow 请求线程中读写上下文。</p>
 * <p>3. 替换默认路由匹配器，使其使用当前工程提供的 Ant 风格路径匹配逻辑。</p>
 * <p>4. 触发 Sa-Token 插件体系初始化，确保注解、策略等扩展点可用。</p>
 *
 * <p>插件停止时会恢复原有 Sa-Token 全局配置，避免影响同一进程中的其他运行环境。</p>
 */
public class SaTokenAifeiPlugin implements Plugin {

    /**
     * 当前插件实例生效时使用的 Sa-Token 配置。
     */
    private final SaTokenConfig config;
    /**
     * 启动插件前的旧配置，用于 stop 时恢复。
     */
    private SaTokenConfig previousConfig;
    /**
     * 启动插件前的旧上下文实现，用于 stop 时恢复。
     */
    private SaTokenContext previousContext;
    /**
     * 启动插件前的旧路由匹配器，用于 stop 时恢复。
     */
    private SaRouteMatchFunction previousRouteMatcher;
    /**
     * 标记插件是否已完成启动，避免重复 stop 破坏外部状态。
     */
    private boolean started;

    /**
     * 使用默认 {@link SaTokenConfig} 创建插件实例。
     */
    public SaTokenAifeiPlugin() {
        this(new SaTokenConfig());
    }

    /**
     * 使用指定配置创建插件实例。
     *
     * @param config Sa-Token 配置对象，不能为空
     */
    public SaTokenAifeiPlugin(SaTokenConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config can not be null.");
        }
        this.config = config;
    }

    /**
     * 先创建默认配置，再通过回调对配置进行二次定制。
     *
     * @param configurer 配置回调，可为空；为空时保留默认配置
     */
    public SaTokenAifeiPlugin(Consumer<SaTokenConfig> configurer) {
        this();
        if (configurer != null) {
            configurer.accept(this.config);
        }
    }

    /**
     * 启动插件并接管 Sa-Token 全局环境。
     *
     * <p>该方法会先缓存旧的全局状态，再安装当前插件所需的配置、上下文和路由匹配器。
     * 随后通过调用 {@link StpUtil#getLoginType()} 触发 Sa-Token 核心组件初始化，
     * 最后显式初始化插件扩展注册中心。</p>
     */
    @Override
    public void start() {
        previousConfig = SaManager.getConfig();
        previousContext = SaManager.getSaTokenContext();
        previousRouteMatcher = SaStrategy.instance.routeMatcher;
        SaManager.setConfig(config);
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        SaStrategy.instance.routeMatcher = (pattern, path) -> PathPatternMatcher.get(pattern).matches(path);
        StpUtil.getLoginType();
        SaTokenPluginHolder.instance.init();
        started = true;
    }

    /**
     * 停止插件并恢复启动前的 Sa-Token 全局状态。
     *
     * <p>如果插件从未启动，则直接返回。否则会先清理当前线程上下文，
     * 再恢复旧配置、旧上下文实现与旧路由匹配器。</p>
     */
    @Override
    public void stop() {
        if (!started) {
            return;
        }
        SaManager.getSaTokenContext().clearContext();
        SaManager.setConfig(previousConfig);
        SaManager.setSaTokenContext(previousContext);
        SaStrategy.instance.routeMatcher = previousRouteMatcher;
        started = false;
    }

    /**
     * 返回当前插件内部持有的配置对象。
     *
     * @return 当前插件配置
     */
    public SaTokenConfig getConfig() {
        return config;
    }

    /**
     * 从 {@link Prop} 中提取常见的 Sa-Token 配置项并构造配置对象。
     *
     * <p>仅处理以 {@code sa-token.} 为前缀的配置，未出现的键保持 Sa-Token 默认值。</p>
     *
     * @param prop Aifei 配置源，不能为空
     * @return 根据配置源构建的 {@link SaTokenConfig} 对象
     */
    public static SaTokenConfig createConfig(Prop prop) {
        if (prop == null) {
            throw new IllegalArgumentException("prop can not be null.");
        }

        SaTokenConfig config = new SaTokenConfig();
        setIfPresent(prop, "sa-token.token-name", config::setTokenName);
        setIfPresent(prop, "sa-token.timeout", value -> config.setTimeout(Long.parseLong(value)));
        setIfPresent(prop, "sa-token.active-timeout", value -> config.setActiveTimeout(Long.parseLong(value)));
        setIfPresent(prop, "sa-token.dynamic-active-timeout", value -> config.setDynamicActiveTimeout(Boolean.parseBoolean(value)));
        setIfPresent(prop, "sa-token.is-concurrent", value -> config.setIsConcurrent(Boolean.parseBoolean(value)));
        setIfPresent(prop, "sa-token.is-share", value -> config.setIsShare(Boolean.parseBoolean(value)));
        setIfPresent(prop, "sa-token.max-login-count", value -> config.setMaxLoginCount(Integer.parseInt(value)));
        setIfPresent(prop, "sa-token.is-read-body", value -> config.setIsReadBody(Boolean.parseBoolean(value)));
        setIfPresent(prop, "sa-token.is-read-header", value -> config.setIsReadHeader(Boolean.parseBoolean(value)));
        setIfPresent(prop, "sa-token.is-read-cookie", value -> config.setIsReadCookie(Boolean.parseBoolean(value)));
        setIfPresent(prop, "sa-token.is-lasting-cookie", value -> config.setIsLastingCookie(Boolean.parseBoolean(value)));
        setIfPresent(prop, "sa-token.is-write-header", value -> config.setIsWriteHeader(Boolean.parseBoolean(value)));
        setIfPresent(prop, "sa-token.token-style", config::setTokenStyle);
        setIfPresent(prop, "sa-token.auto-renew", value -> config.setAutoRenew(Boolean.parseBoolean(value)));
        setIfPresent(prop, "sa-token.token-prefix", config::setTokenPrefix);
        setIfPresent(prop, "sa-token.is-print", value -> config.setIsPrint(Boolean.parseBoolean(value)));
        setIfPresent(prop, "sa-token.is-log", value -> config.setIsLog(Boolean.parseBoolean(value)));
        setIfPresent(prop, "sa-token.log-level", config::setLogLevel);
        setIfPresent(prop, "sa-token.jwt-secret-key", config::setJwtSecretKey);
        setIfPresent(prop, "sa-token.curr-domain", config::setCurrDomain);
        return config;
    }

    /**
     * 当配置键存在时执行赋值操作。
     *
     * @param prop 配置源
     * @param key 配置键
     * @param setter 配置值写入逻辑
     */
    private static void setIfPresent(Prop prop, String key, Consumer<String> setter) {
        String value = prop.get(key);
        if (value != null) {
            setter.accept(value);
        }
    }
}
