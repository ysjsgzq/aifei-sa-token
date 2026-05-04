# aifei-sa-token

[![Build](https://github.com/ysjsgzq/aifei-sa-token/actions/workflows/build.yml/badge.svg)](https://github.com/ysjsgzq/aifei-sa-token/actions/workflows/build.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](./LICENSE)

`aifei-sa-token` 是一个面向 [Aifei](https://github.com/jfinal/aifei) 的低侵入 `Sa-Token` 集成插件，用来在不改动现有 HIO 设计的前提下，把 `Sa-Token` 接入到 `Aifei + Undertow` 项目里。

它的目标很明确：

- 不修改 `Input` / `Output`
- 不引入自定义 `Sa-Token HIO`
- 不要求重写现有 `ActionHandler`
- 只在 Undertow `Dispatcher` 外层做一次上下文绑定与清理

## 适用场景

如果你的项目满足下面这些条件，这个插件就是为你准备的：

- 使用 `Aifei`
- 使用 `Undertow`
- 想接入 `Sa-Token`
- 不想改动现有控制层、输入输出模型和分发结构

## 核心组件

- `SaTokenAifeiPlugin`
  - 初始化 `Sa-Token` 配置
  - 注册线程级上下文实现
  - 安装适用于当前项目的路由匹配器
- `SaAnnotationInterceptor`
  - 只处理 `@SaCheckLogin`、`@SaCheckRole`、`@SaCheckPermission` 等注解鉴权
- `SaTokenInterceptor`
  - 处理 `include` / `exclude` 路径规则
  - 支持 `beforeAuth`、自定义 `auth`、统一错误处理
  - 默认可继续叠加方法注解鉴权
- `SaTokenUndertowDispatcher`
  - 为每个 Undertow 请求绑定 `Sa-Token` 的 `Request`、`Response`、`Storage` 上下文

## 版本信息

当前 `pom.xml` 中使用的依赖版本：

- `Aifei 1.0.0`
- `aifei-undertow 1.0.0`
- `Sa-Token 1.45.0`
- `JDK 8+`

## Maven 依赖

```xml
<dependency>
    <groupId>cn.aifei</groupId>
    <artifactId>aifei-sa-token</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 接入步骤

### 1. 注册插件

推荐通过 `Prop` 读取 `Sa-Token` 配置，再注册 `SaTokenAifeiPlugin`。

```java
@Override
public void config(Plugins plugins) {
    Prop prop = PropKit.use("app-config.txt");
    plugins.add(new SaTokenAifeiPlugin(SaTokenAifeiPlugin.createConfig(prop)));
}
```

### 2. 包装 Undertow Dispatcher

保留你现有的 HIO 设计，只需要用 `SaTokenUndertowDispatcher` 包一层当前 `Dispatcher`。

```java
@Override
public void config(Settings<Input, Output> settings) {
    settings.setServer(
        new UndertowServer(),
        new SaTokenUndertowDispatcher<>(new Dispatcher())
    );
    settings.addHandler(new ActionHandler());
}
```

### 3. 选择拦截器接入方式

你通常只需要下面两种方式中的一种。

#### 方式一：只用注解鉴权

```java
@Override
public void config(Routes routes) {
    routes.scan("demo.service", new SaAnnotationInterceptor());
}
```

#### 方式二：按路径统一鉴权

```java
@Override
public void config(Routes routes) {
    routes.scan(
        "demo.service",
        new SaTokenInterceptor()
            .addInclude("/**")
            .addExclude("/auth/login")
    );
}
```

## `SaTokenInterceptor` 与 `SaAnnotationInterceptor` 的区别

这两个拦截器不是重复实现，而是面向两种不同的接入方式。

- `SaAnnotationInterceptor`
  - 只做方法注解鉴权
  - 只检查当前方法上的 `@SaCheckLogin`、`@SaCheckRole`、`@SaCheckPermission` 等注解
  - 不处理路径匹配规则
- `SaTokenInterceptor`
  - 是综合拦截器
  - 支持 `include` / `exclude` 路径匹配
  - 支持 `beforeAuth` 前置处理
  - 支持自定义 `auth` 鉴权逻辑
  - 默认还会执行方法注解鉴权

可以这样选：

- 只想用注解鉴权时，用 `SaAnnotationInterceptor`
- 想按路由统一做鉴权时，用 `SaTokenInterceptor`
- 想同时支持“路径规则 + 注解规则”时，通常只用 `SaTokenInterceptor` 就够了

注意：

- 一般不要同时注册这两个拦截器
- 因为 `SaTokenInterceptor` 默认已经会检查注解
- 如果再单独注册 `SaAnnotationInterceptor`，同一个方法上的注解鉴权可能会执行两次
- 如果确实要同时挂载，请把 `SaTokenInterceptor.isAnnotation` 设为 `false`

## 配置项

`SaTokenAifeiPlugin.createConfig(prop)` 支持从 `Prop` 读取常用配置：

```properties
sa-token.token-name = demo-token
sa-token.timeout = 86400
sa-token.is-read-header = true
sa-token.is-write-header = true
sa-token.token-prefix = Bearer
```

更多配置参考 [Sa-Token 配置文档](https://sa-token.cc/doc.html#/use/config)。

## 角色与权限

角色和权限由 `StpInterface` 提供：

```java
public class AppStpInterface implements StpInterface {

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return permissionService.listByUserId(loginId);
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return roleService.listByUserId(loginId);
    }
}
```

在启动阶段注册：

```java
@Override
public void onStart() {
    SaManager.setStpInterface(Aop.get(AppStpInterface.class));
}
```

## 注解鉴权示例

```java
@Path("/article")
public class ArticleService {

    @SaCheckLogin
    public Object index() {
        return "ok";
    }

    @SaCheckRole("admin")
    public Object adminOnly() {
        return "ok";
    }

    @SaCheckPermission("article.write")
    public Object save(String title, String content) {
        return "ok";
    }
}
```

## 路径鉴权示例

```java
routes.scan(
    "demo.service",
    new SaTokenInterceptor()
        .addInclude("/admin/**")
        .addExclude("/auth/login", "/auth/doLogin")
        .setAuth(inv -> StpUtil.checkLogin())
);
```

## 一个完整示例

```java
public class AppConfig implements AifeiConfig<Input, Output> {

    private Prop prop;

    @Override
    public void config(Settings<Input, Output> settings) {
        settings.setServer(
            new UndertowServer(),
            new SaTokenUndertowDispatcher<>(new Dispatcher())
        );
        settings.addHandler(new ActionHandler());
    }

    @Override
    public void config(Routes routes) {
        routes.scan("demo.service", new SaAnnotationInterceptor());
    }

    @Override
    public void config(Plugins plugins) {
        prop = PropKit.use("app-config.txt");
        plugins.add(new SaTokenAifeiPlugin(SaTokenAifeiPlugin.createConfig(prop)));
    }

    @Override
    public void onStart() {
        SaManager.setStpInterface(Aop.get(AppStpInterface.class));
    }
}
```

## 构建

```bash
mvn clean package
```

## 致谢

感谢以下优秀项目为本插件提供基础能力与设计启发：

- [jfinal/aifei](https://github.com/jfinal/aifei)
- [dromara/sa-token](https://github.com/dromara/sa-token)

## License

Apache License 2.0
