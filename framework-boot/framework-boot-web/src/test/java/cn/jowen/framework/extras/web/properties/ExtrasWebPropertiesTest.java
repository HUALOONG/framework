package cn.jowen.framework.extras.web.properties;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 覆盖 {@link ExtrasWebProperties} 全部 getter/setter（含各内嵌配置子类）。
 */
class ExtrasWebPropertiesTest {

    @Test
    void gettersAndSetters() {
        ExtrasWebProperties p = new ExtrasWebProperties();
        assertThat(p.isEnabled()).isTrue();
        p.setEnabled(false);
        assertThat(p.isEnabled()).isFalse();

        ExtrasWebProperties.Sign sign = new ExtrasWebProperties.Sign();
        assertThat(sign.isEnabled()).isTrue();
        sign.setEnabled(false);
        assertThat(sign.isEnabled()).isFalse();
        Map<String, String> secrets = new LinkedHashMap<>();
        secrets.put("app1", "s1");
        sign.setAppSecrets(secrets);
        assertThat(sign.getAppSecrets()).containsEntry("app1", "s1");
        p.setSign(sign);
        assertThat(p.getSign()).isSameAs(sign);

        ExtrasWebProperties.Crypto crypto = new ExtrasWebProperties.Crypto();
        assertThat(crypto.isEnabled()).isTrue();
        crypto.setEnabled(false);
        assertThat(crypto.isEnabled()).isFalse();
        assertThat(crypto.getDefaultKeyAlias()).isEqualTo("default");
        crypto.setDefaultKeyAlias("k");
        assertThat(crypto.getDefaultKeyAlias()).isEqualTo("k");
        Map<String, String> keys = new LinkedHashMap<>();
        crypto.setKeys(keys);
        assertThat(crypto.getKeys()).isSameAs(keys);
        p.setCrypto(crypto);
        assertThat(p.getCrypto()).isSameAs(crypto);

        ExtrasWebProperties.RateLimit rl = new ExtrasWebProperties.RateLimit();
        assertThat(rl.isEnabled()).isTrue();
        rl.setEnabled(false);
        assertThat(rl.isEnabled()).isFalse();
        p.setRateLimit(rl);
        assertThat(p.getRateLimit()).isSameAs(rl);

        ExtrasWebProperties.Idempotent id = new ExtrasWebProperties.Idempotent();
        assertThat(id.isEnabled()).isTrue();
        id.setEnabled(false);
        assertThat(id.isEnabled()).isFalse();
        p.setIdempotent(id);
        assertThat(p.getIdempotent()).isSameAs(id);

        ExtrasWebProperties.Lock lock = new ExtrasWebProperties.Lock();
        assertThat(lock.isEnabled()).isTrue();
        assertThat(lock.getDefaultLeaseMillis()).isEqualTo(30000L);
        lock.setEnabled(false);
        lock.setDefaultLeaseMillis(1000L);
        assertThat(lock.getDefaultLeaseMillis()).isEqualTo(1000L);
        p.setLock(lock);
        assertThat(p.getLock()).isSameAs(lock);

        ExtrasWebProperties.Captcha cap = new ExtrasWebProperties.Captcha();
        assertThat(cap.isEnabled()).isFalse();
        assertThat(cap.getType()).isEqualTo(ExtrasWebProperties.Captcha.CaptchaType.ARITHMETIC);
        cap.setEnabled(true);
        cap.setType(ExtrasWebProperties.Captcha.CaptchaType.GRAPHIC);
        assertThat(cap.getType()).isEqualTo(ExtrasWebProperties.Captcha.CaptchaType.GRAPHIC);
        cap.setExpireSeconds(60);
        assertThat(cap.getExpireSeconds()).isEqualTo(60);
        cap.setLength(6);
        assertThat(cap.getLength()).isEqualTo(6);
        cap.setWidth(200);
        assertThat(cap.getWidth()).isEqualTo(200);
        cap.setHeight(80);
        assertThat(cap.getHeight()).isEqualTo(80);
        p.setCaptcha(cap);
        assertThat(p.getCaptcha()).isSameAs(cap);

        ExtrasWebProperties.DataPermission dp = new ExtrasWebProperties.DataPermission();
        assertThat(dp.isEnabled()).isFalse();
        assertThat(dp.getDeptColumn()).isEqualTo("dept_id");
        assertThat(dp.getUserColumn()).isEqualTo("create_by");
        dp.setEnabled(true);
        dp.setDeptColumn("d");
        dp.setUserColumn("u");
        assertThat(dp.getDeptColumn()).isEqualTo("d");
        assertThat(dp.getUserColumn()).isEqualTo("u");
        p.setDataPermission(dp);
        assertThat(p.getDataPermission()).isSameAs(dp);

        ExtrasWebProperties.OperateLog ol = new ExtrasWebProperties.OperateLog();
        assertThat(ol.isEnabled()).isFalse();
        assertThat(ol.isAsync()).isTrue();
        assertThat(ol.getHandler()).isEqualTo("log");
        ol.setEnabled(true);
        ol.setAsync(false);
        ol.setHandler("db");
        assertThat(ol.getHandler()).isEqualTo("db");
        p.setOperateLog(ol);
        assertThat(p.getOperateLog()).isSameAs(ol);

        ExtrasWebProperties.Notification no = new ExtrasWebProperties.Notification();
        assertThat(no.isEnabled()).isFalse();
        no.setEnabled(true);
        p.setNotification(no);
        assertThat(p.getNotification()).isSameAs(no);

        ExtrasWebProperties.Desensitize de = new ExtrasWebProperties.Desensitize();
        assertThat(de.isEnabled()).isFalse();
        de.setEnabled(true);
        p.setDesensitize(de);
        assertThat(p.getDesensitize()).isSameAs(de);

        ExtrasWebProperties.Excel ex = new ExtrasWebProperties.Excel();
        assertThat(ex.isEnabled()).isFalse();
        assertThat(ex.getMaxRows()).isEqualTo(100000);
        ex.setEnabled(true);
        ex.setMaxRows(50);
        assertThat(ex.getMaxRows()).isEqualTo(50);
        assertThatThrownBy(() -> ex.setMaxRows(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxRows");
        p.setExcel(ex);
        assertThat(p.getExcel()).isSameAs(ex);

        ExtrasWebProperties.Ip2Region ip = new ExtrasWebProperties.Ip2Region();
        assertThat(ip.isEnabled()).isFalse();
        ip.setEnabled(true);
        p.setIp2Region(ip);
        assertThat(p.getIp2Region()).isSameAs(ip);
    }
}
