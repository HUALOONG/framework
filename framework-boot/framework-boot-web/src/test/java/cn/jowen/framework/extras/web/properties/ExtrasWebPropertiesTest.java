package cn.jowen.framework.extras.web.properties;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ExtrasWebProperties} 默认值与绑定契约测试。
 *
 * <p>该类的可绑定性依赖"非 final 嵌套类 + 无参构造 + 标准 setter"，
 * 默认值同时决定未配置时的框架行为，故对二者加锁保护。
 */
class ExtrasWebPropertiesTest {

    @Test
    void root_isEnabledByDefault() {
        assertThat(new ExtrasWebProperties().isEnabled()).isTrue();
    }

    @Test
    void allNestedSections_areInitialized() {
        ExtrasWebProperties props = new ExtrasWebProperties();

        assertThat(props.getCrypto()).isNotNull();
        assertThat(props.getSign()).isNotNull();
        assertThat(props.getRateLimit()).isNotNull();
        assertThat(props.getIdempotent()).isNotNull();
        assertThat(props.getLock()).isNotNull();
        assertThat(props.getCaptcha()).isNotNull();
        assertThat(props.getDataPermission()).isNotNull();
        assertThat(props.getOperateLog()).isNotNull();
        assertThat(props.getNotification()).isNotNull();
        assertThat(props.getDesensitize()).isNotNull();
        assertThat(props.getExcel()).isNotNull();
        assertThat(props.getIp2Region()).isNotNull();
    }

    @Test
    void defaultEnabledFlags_matchContract() {
        ExtrasWebProperties props = new ExtrasWebProperties();

        // 默认开启：切面在有注解时才生效，故安全
        assertThat(props.getCrypto().isEnabled()).as("crypto").isTrue();
        assertThat(props.getSign().isEnabled()).as("sign").isTrue();
        assertThat(props.getRateLimit().isEnabled()).as("rateLimit").isTrue();
        assertThat(props.getIdempotent().isEnabled()).as("idempotent").isTrue();
        assertThat(props.getLock().isEnabled()).as("lock").isTrue();

        // 默认关闭：需要额外资源或业务实现
        assertThat(props.getCaptcha().isEnabled()).as("captcha").isFalse();
        assertThat(props.getDataPermission().isEnabled()).as("dataPermission").isFalse();
        assertThat(props.getOperateLog().isEnabled()).as("operateLog").isFalse();
        assertThat(props.getNotification().isEnabled()).as("notification").isFalse();
        assertThat(props.getDesensitize().isEnabled()).as("desensitize").isFalse();
        assertThat(props.getExcel().isEnabled()).as("excel").isFalse();
        assertThat(props.getIp2Region().isEnabled()).as("ip2Region").isFalse();
    }

    @Test
    void crypto_defaults() {
        ExtrasWebProperties.Crypto crypto = new ExtrasWebProperties.Crypto();

        assertThat(crypto.isEnabled()).isTrue();
        assertThat(crypto.getDefaultKeyAlias()).isEqualTo("default");
        assertThat(crypto.getKeys()).isNotNull().isEmpty();
    }

    @Test
    void sign_defaults() {
        ExtrasWebProperties.Sign sign = new ExtrasWebProperties.Sign();

        assertThat(sign.isEnabled()).isTrue();
        assertThat(sign.getAppSecrets()).isNotNull().isEmpty();
    }

    @Test
    void captcha_defaults() {
        ExtrasWebProperties.Captcha captcha = new ExtrasWebProperties.Captcha();

        assertThat(captcha.isEnabled()).isFalse();
        assertThat(captcha.getType()).isEqualTo(ExtrasWebProperties.Captcha.CaptchaType.ARITHMETIC);
        assertThat(captcha.getWidth()).isEqualTo(120);
        assertThat(captcha.getHeight()).isEqualTo(40);
        assertThat(captcha.getLength()).isEqualTo(4);
        assertThat(captcha.getExpireSeconds()).isEqualTo(120L);
    }

    @Test
    void operateLog_defaults() {
        ExtrasWebProperties.OperateLog operateLog = new ExtrasWebProperties.OperateLog();

        assertThat(operateLog.isEnabled()).isFalse();
        assertThat(operateLog.isAsync()).isTrue();
        assertThat(operateLog.getHandler()).isEqualTo("log");
    }

    @Test
    void dataPermission_defaults() {
        ExtrasWebProperties.DataPermission dataPermission = new ExtrasWebProperties.DataPermission();

        assertThat(dataPermission.isEnabled()).isFalse();
        assertThat(dataPermission.getDeptColumn()).isEqualTo("dept_id");
        assertThat(dataPermission.getUserColumn()).isEqualTo("create_by");
    }

    @Test
    void captchaType_enumIsComplete() {
        assertThat(ExtrasWebProperties.Captcha.CaptchaType.values()).containsExactly(
                ExtrasWebProperties.Captcha.CaptchaType.GRAPHIC,
                ExtrasWebProperties.Captcha.CaptchaType.ARITHMETIC,
                ExtrasWebProperties.Captcha.CaptchaType.SLIDER,
                ExtrasWebProperties.Captcha.CaptchaType.SMS);
    }

    @Test
    void dataPermission_columnsCanBeCustomized() {
        ExtrasWebProperties.DataPermission dataPermission = new ExtrasWebProperties.DataPermission();
        dataPermission.setDeptColumn("org_id");
        dataPermission.setUserColumn("owner");

        assertThat(dataPermission.getDeptColumn()).isEqualTo("org_id");
        assertThat(dataPermission.getUserColumn()).isEqualTo("owner");
    }

    // ---------- 绑定语义 ----------

    @Test
    void nestedSections_areReplaceableViaSetter() {
        ExtrasWebProperties props = new ExtrasWebProperties();
        ExtrasWebProperties.Crypto crypto = new ExtrasWebProperties.Crypto();
        crypto.setDefaultKeyAlias("custom");
        crypto.setKeys(Map.of("custom", "base64key"));

        props.setCrypto(crypto);

        assertThat(props.getCrypto()).isSameAs(crypto);
        assertThat(props.getCrypto().getDefaultKeyAlias()).isEqualTo("custom");
        assertThat(props.getCrypto().getKeys()).containsEntry("custom", "base64key");
    }

    @Test
    void nestedSectionMutation_isVisibleThroughRoot() {
        ExtrasWebProperties props = new ExtrasWebProperties();
        props.getCaptcha().setType(ExtrasWebProperties.Captcha.CaptchaType.SLIDER);
        props.getCaptcha().setExpireSeconds(300L);
        props.getSign().getAppSecrets().put("app", "secret");

        assertThat(props.getCaptcha().getType())
                .isEqualTo(ExtrasWebProperties.Captcha.CaptchaType.SLIDER);
        assertThat(props.getCaptcha().getExpireSeconds()).isEqualTo(300L);
        assertThat(props.getSign().getAppSecrets()).containsEntry("app", "secret");
    }

    @Test
    void instances_areIndependent() {
        ExtrasWebProperties a = new ExtrasWebProperties();
        ExtrasWebProperties b = new ExtrasWebProperties();

        a.getCaptcha().setEnabled(true);
        a.getSign().getAppSecrets().put("app", "secret");

        assertThat(b.getCaptcha().isEnabled()).isFalse();
        assertThat(b.getSign().getAppSecrets()).isEmpty();
    }

    @Test
    void root_enabledCanBeDisabled() {
        ExtrasWebProperties props = new ExtrasWebProperties();
        props.setEnabled(false);

        assertThat(props.isEnabled()).isFalse();
    }
}
