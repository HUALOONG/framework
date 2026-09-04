package cn.jowen.framework.data.mybatis.extension;

import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

@NullMarked
/**
 * 「FlexEncrypt」封装相关能力。
 *
 * @author Jowen
 * @since 0.0.1
 * @version 0.0.1
 */
public class FlexEncryptProcessor implements ExtensionRegistry.Extension {

    /** logger 常量。 */
    private static final Logger logger = LoggerFactory.getLogger(FlexEncryptProcessor.class);

    /** ALGORITHM 常量。 */
    private static final String ALGORITHM = "AES";
    /** 实体类 → 需要加解密的字段（{@link Encrypted} 标注的 String 字段）缓存。 */
    private static final ConcurrentMap<Class<?>, List<Field>> ENCRYPTED_FIELDS = new ConcurrentHashMap<>();
    /** key 静态变量。 */
    private static volatile @Nullable String key = "";

    /** return 字段。 */
    @Override public String name() { return "encrypt"; }
    /** return 字段。 */
    @Override public int order() { return 300; }

    /**
     * 设置全局加密密钥（Base64 编码的 AES 密钥）。传 {@code null} 表示关闭透明加解密。
     *
     * @param base64Key 密钥，可为 {@code null}
     */
    public static void setDefaultKey(@Nullable String base64Key) { key = base64Key; }

    /**
     * 是否已配置加密密钥。未配置时 {@link #encryptEntity(Object)} / {@link #decryptEntity(Object)} 为安全空操作。
     *
     * @return 已配置返回 {@code true}
     */
    public boolean isConfigured() {
        String current = key;
        return current != null && !current.isEmpty();
    }

    /**
     * 对实体中所有 {@link Encrypted} 标注的 {@link String} 字段执行加密（落库前调用）。
     * 未配置密钥或实体为 {@code null} 时为空操作。
     *
     * @param entity 实体对象，可为 {@code null}
     */
    public void encryptEntity(@Nullable Object entity) {
        applyEach(entity, true);
    }

    /**
     * 对实体中所有 {@link Encrypted} 标注的 {@link String} 字段执行解密（读取后调用）。
     * 未配置密钥或实体为 {@code null} 时为空操作；解密失败时保留原值。
     *
     * @param entity 实体对象，可为 {@code null}
     */
    public void decryptEntity(@Nullable Object entity) {
        applyEach(entity, false);
    }

    private void applyEach(@Nullable Object entity, boolean encrypt) {
        if (entity == null || !isConfigured()) return;
        for (Field field : encryptedFields(entity.getClass())) {
            try {
                field.setAccessible(true);
                Object value = field.get(entity);
                if (value instanceof String text) {
                    field.set(entity, encrypt ? encrypt(text) : decrypt(text));
                }
            } catch (Exception e) {
                logger.debug("字段" + (encrypt ? "加密" : "解密") + "失败 (" + field.getName() + "): " + e.getMessage());
            }
        }
    }

    private static List<Field> encryptedFields(Class<?> type) {
        return ENCRYPTED_FIELDS.computeIfAbsent(type, t -> {
            List<Field> found = new ArrayList<>();
            for (Class<?> c = t; c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field field : c.getDeclaredFields()) {
                    if (field.isAnnotationPresent(Encrypted.class) && field.getType() == String.class) {
                        found.add(field);
                    }
                }
            }
            return List.copyOf(found);
        });
    }

    /**
     * 执行encrypt操作。
     * @param plain 参数 plain
     * @return 结果
     */
    @Nullable
    public String encrypt(@Nullable String plain) {
        if (plain == null || plain.isEmpty()) return plain;
        try {
            byte[] decodedKey = Base64.getDecoder().decode(resolveKey());
            SecretKeySpec spec = new SecretKeySpec(decodedKey, ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM + "/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, spec);
            return Base64.getEncoder().encodeToString(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return plain;
        }
    }

    /**
     * 执行decrypt操作。
     * @param cipherText 参数 cipherText
     * @return 结果
     */
    @Nullable
    public String decrypt(@Nullable String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) return cipherText;
        try {
            byte[] decodedKey = Base64.getDecoder().decode(resolveKey());
            SecretKeySpec spec = new SecretKeySpec(decodedKey, ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM + "/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, spec);
            return new String(cipher.doFinal(Base64.getDecoder().decode(cipherText)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return cipherText;
        }
    }

    private @Nullable String resolveKey() {
        return key;
    }

    /** FieldEncryptor 字段。 */
    @FunctionalInterface
    /**
     * 「FieldEncryptor」接口定义。
     *
     * @author Jowen
     * @since 0.0.1
 * @version 0.0.1
     */
    public interface FieldEncryptor {
        String encrypt(String plain);

        /**
         * 解密单个字段值。
         *
         * <p>默认实现为<b>扩展点占位</b>：故意抛出 {@link UnsupportedOperationException}，
         * 引导自定义 {@code FieldEncryptor} 覆盖本方法提供具体解密策略。
         * 框架内置的透明加解密由 {@link FlexEncryptProcessor#decrypt(String)} 提供，
         * 其本身已实现（AES/ECB），无需实现本接口即可工作。
         *
         * @param cipher 密文，不可为 {@code null}
         * @return 明文
         */
        default String decrypt(String cipher) { throw new UnsupportedOperationException("decrypt not implemented"); }
    }
}
