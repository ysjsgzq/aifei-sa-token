# aifei-sa-token

`aifei-sa-token` 是一个面向 [Aifei](https://github.com/jfinal/aifei) 的低侵入 `Sa-Token` 集成插件。

它的设计目标很明确：

- 不修改 `Input` / `Output`
- 不引入自定义 `Sa-Token HIO`
- 不要求重写现有 `ActionHandler`
- 只在 Undertow `Dispatcher` 外层做一次上下文绑定与清理

## 特性

- `SaTokenAifeiPlugin`：初始化 `Sa-Token`
- `SaAnnotationInterceptor`：支持注解鉴权
- `SaTokenInterceptor`：支持路径规则鉴权，并可选叠加注解鉴权
- `SaTokenUndertowDispatcher`：为 Undertow 请求绑定 `Sa-Token` 上下文

## Maven 依赖

```xml
<dependency>
    <groupId>cn.aifei</groupId>
    <artifactId>aifei-sa-token</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 快速开始

保留你现有的 HIO 设计，只需要用 `SaTokenUndertowDispatcher` 包一层当前 `Dispatcher`。

```java
public class AppConfig implements AifeiConfig<Input, OutPut> {

    private Prop prop;

    @Override
    public void config(Settings<Input, OutPut> settings) {
        prop = PropKit.use("app-config.txt");

        settings.setServer(
            new UndertowServer(),
            new SaTokenUndertowDispatcher<>(new Dispatcher())
        );
        settings.addHandler(new ActionHandler());
    }

    @Override
    public void config(Routes routes) {
        //注解鉴权
        routes.scan("demo.service", new SaAnnotationInterceptor());
        
        //综合拦截器。它支持 include / exclude 路径匹配、beforeAuth 前置处理、自定义 auth 鉴权逻辑，并且默认还会执行方法注解鉴权
        //routes.scan("demo.service", new SaTokenInterceptor());
    }

    @Override
    public void config(Plugins plugins) {
        plugins.add(new SaTokenAifeiPlugin(SaTokenAifeiPlugin.createConfig(prop)));
    }
}
```

## 配置项

`SaTokenAifeiPlugin.createConfig(prop)` 支持从 `Prop` 读取常用配置：

```properties
sa-token.token-name = demo-token
sa-token.timeout = 86400
sa-token.is-read-header = true
sa-token.is-write-header = true
sa-token.token-prefix = Bearer
//更多配置参考：https://sa-token.cc/doc.html#/use/config
```

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

## `SaTokenInterceptor` 与 `SaAnnotationInterceptor` 的区别

这两个拦截器不是重复实现，而是面向两种不同的接入方式：

- `SaAnnotationInterceptor`：只做方法注解鉴权。它只检查当前方法上的 `@SaCheckLogin`、`@SaCheckRole`、`@SaCheckPermission` 等注解，不处理路径匹配规则。
- `SaTokenInterceptor`：是综合拦截器。它支持 `include` / `exclude` 路径匹配、`beforeAuth` 前置处理、自定义 `auth` 鉴权逻辑，并且默认还会执行方法注解鉴权。

可以这样理解：

- 只想用注解鉴权时，用 `SaAnnotationInterceptor`
- 想按路由统一做鉴权时，用 `SaTokenInterceptor`
- 想同时支持“路径规则 + 注解规则”时，通常只用 `SaTokenInterceptor` 就够了

注意：

- 一般不要同时注册这两个拦截器。
- 因为 `SaTokenInterceptor` 默认已经会检查注解，如果再单独注册 `SaAnnotationInterceptor`，同一个方法上的注解鉴权可能会执行两次。
- 如果确实要同时挂载，请把 `SaTokenInterceptor.isAnnotation` 设为 `false`。

## 路径鉴权示例

```java
routes.scan(
    "demo.service",
    new SaTokenInterceptor()
        .addInclude("/**")
        .addExclude("/auth/login")
);
```

## 构建

```bash
mvn clean package
```

## License

Apache License 2.0
