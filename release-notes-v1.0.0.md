# v1.0.0

`aifei-sa-token` 首个公开版本发布。

## Highlights

- 为 `Aifei + Undertow` 提供低侵入 `Sa-Token` 集成方案
- 不修改现有 `Input` / `Output`
- 不引入自定义 `Sa-Token HIO`
- 不要求重写现有 `ActionHandler`
- 只在 Undertow `Dispatcher` 外层完成上下文绑定与清理

## Included

- `SaTokenAifeiPlugin`
  - 初始化 `Sa-Token` 配置
  - 注册线程级上下文实现
  - 安装适配当前项目的路由匹配器
- `SaAnnotationInterceptor`
  - 支持 `@SaCheckLogin`、`@SaCheckRole`、`@SaCheckPermission` 等注解鉴权
- `SaTokenInterceptor`
  - 支持 `include` / `exclude` 路径匹配
  - 支持 `beforeAuth`
  - 支持自定义 `auth` 鉴权逻辑
  - 默认支持注解鉴权叠加
- `SaTokenUndertowDispatcher`
  - 为 Undertow 请求绑定 `Sa-Token` 上下文
- Undertow bridge 实现
  - `SaRequestForUndertowBridge`
  - `SaResponseForUndertowBridge`
  - `SaStorageForUndertowBridge`

## Maven Central

```xml
<dependency>
    <groupId>io.github.ysjsgzq</groupId>
    <artifactId>aifei-sa-token</artifactId>
    <version>1.0.0</version>
</dependency>
```

Maven Central:

- https://repo.maven.apache.org/maven2/io/github/ysjsgzq/aifei-sa-token/1.0.0/

## Notes

- Git tag: `v1.0.0`
- Java baseline: `JDK 8+`

## Thanks

- [jfinal/aifei](https://github.com/jfinal/aifei)
- [dromara/sa-token](https://github.com/dromara/sa-token)
