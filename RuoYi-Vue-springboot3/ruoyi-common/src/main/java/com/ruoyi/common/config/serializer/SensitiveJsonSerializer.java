package com.ruoyi.common.config.serializer;

import java.io.IOException;
import java.util.Objects;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.ruoyi.common.annotation.Sensitive;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.enums.DesensitizedType;
import com.ruoyi.common.utils.SecurityUtils;

/**
 * 数据脱敏序列化过滤
 *
 * @author ruoyi
 *
 * NOTE: /筆記/jackson/Jackson 自定义序列化器的使用.md
 *
 * 先把關鍵結論講在前面：
 * serialize 是「真的動手把值寫進 JSON」的方法，沒有它 Jackson 根本不知道要怎麼輸出。
 * createContextual 是「在序列化之前，針對每一個欄位替序列化器做欄位級設定」的方法，讓同一個類可以依欄位上的註解（@Sensitive）有不同行為。
 *
 * 這個脫敏需求本質上是兩件事：
 * 1. 先看欄位上 @Sensitive 註解 → 取出 desensitizedType
 * 2. 在真正輸出時，按 desensitizedType 去遮罩字串，且依目前登入身分（admin or not）決定要不要遮
 *
 * Jackson 的設計就是用：
 * createContextual 處理「第 1 步（欄位設定）」
 * serialize 處理「第 2 步（實際輸出）」
 * 所以兩個方法是分工，不是重複。
 */
public class SensitiveJsonSerializer extends JsonSerializer<String> implements ContextualSerializer
{
    private DesensitizedType desensitizedType;

    /**
     * 這裡只有：
     * 1. `value`：欄位的值（例如 `"0912345678"`）
     * 2. `gen`：寫 JSON 的工具
     * 3. `serializers`：一些共用的 provider
     * 拿不到 BeanProperty、本欄位上的註解、欄位名稱等 metadata。
     * 也就是說，如果你只靠 `serialize`：
     * 看不到 `@Sensitive(desensitizedType = XXX)` 長怎樣
     * 也不知道 **這次的 value 是哪一個欄位**，也不知道要用哪一種脫敏規則
     */
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        if (desensitization())
        {
            gen.writeString(desensitizedType.desensitizer().apply(value));
        }
        else
        {
            gen.writeString(value);
        }
    }

    /**
     * `createContextual` 只負責一件事：**決定最後要用哪一個序列化器實例**。
     * 這裡做的事是：
     * 1. 讀欄位上的 `@Sensitive`
     * 2. 如果適用，就設定 `this.desensitizedType`
     * 3. 然後「把這個序列化器（或新的實例）交回 Jackson」
     */
    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
            throws JsonMappingException
    {
        // 嘗試從欄位上取得 @Sensitive 註解。
        Sensitive annotation = property.getAnnotation(Sensitive.class);

        // 確保這個欄位的型別是 String，只有 String 欄位才允許用這個脫敏序列化器。
        if (Objects.nonNull(annotation) && Objects.equals(String.class, property.getType().getRawClass()))
        {
            // 有 @Sensitive，且型別是 String
            this.desensitizedType = annotation.desensitizedType();
            return this;
        }
        // 交給 Jackson 去找該型別的預設序列化器（不做脫敏）。
        return prov.findValueSerializer(property.getType(), property);
    }

    /**
     * 是否需要脱敏处理
     */
    private boolean desensitization()
    {
        try
        {
            LoginUser securityUser = SecurityUtils.getLoginUser();
            // 管理员不脱敏
            return !securityUser.getUser().isAdmin();
        }
        catch (Exception e)
        {
            return true;
        }
    }
}
