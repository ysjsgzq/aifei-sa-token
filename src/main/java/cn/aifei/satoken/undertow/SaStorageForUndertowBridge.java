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

package cn.aifei.satoken.undertow;

import cn.dev33.satoken.context.model.SaStorage;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 基于 Undertow Attachment 的 Sa-Token 存储桥接实现。
 *
 * <p>Sa-Token 需要一个请求级别的键值存储来保存处理中间数据。
 * 这里直接复用 Undertow 的 Attachment 机制，使数据生命周期与
 * {@link HttpServerExchange} 保持一致。</p>
 */
class SaStorageForUndertowBridge implements SaStorage {

    /**
     * Undertow 附件键，用于挂载请求级存储 Map。
     */
    private static final AttachmentKey<Map<String, Object>> STORAGE_KEY = AttachmentKey.create(Map.class);

    /**
     * 当前请求对应的 Undertow 交换对象。
     */
    private final HttpServerExchange exchange;

    /**
     * 创建请求级存储桥接对象。
     *
     * @param exchange 当前 Undertow 交换对象
     */
    SaStorageForUndertowBridge(HttpServerExchange exchange) {
        this.exchange = exchange;
    }

    /**
     * 返回底层存储源对象。
     *
     * @return 当前 {@link HttpServerExchange}
     */
    @Override
    public Object getSource() {
        return exchange;
    }

    /**
     * 从请求级存储中读取数据。
     *
     * @param key 存储键
     * @return 对应值
     */
    @Override
    public Object get(String key) {
        return getStorageMap().get(key);
    }

    /**
     * 向请求级存储写入数据。
     *
     * @param key 存储键
     * @param value 存储值
     * @return 当前存储对象
     */
    @Override
    public SaStorage set(String key, Object value) {
        getStorageMap().put(key, value);
        return this;
    }

    /**
     * 删除请求级存储中的指定键。
     *
     * @param key 存储键
     * @return 当前存储对象
     */
    @Override
    public SaStorage delete(String key) {
        getStorageMap().remove(key);
        return this;
    }

    /**
     * 读取底层存储 Map，不存在时按需创建。
     *
     * @return 当前请求对应的存储 Map
     */
    private Map<String, Object> getStorageMap() {
        Map<String, Object> storage = exchange.getAttachment(STORAGE_KEY);
        if (storage == null) {
            storage = new LinkedHashMap<>();
            exchange.putAttachment(STORAGE_KEY, storage);
        }
        return storage;
    }
}
