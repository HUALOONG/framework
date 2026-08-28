package cn.jowen.framework.extras.web.captcha;

import cn.jowen.framework.extras.web.properties.ExtrasWebProperties;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 验证码服务：按配置类型生成验证码，并支持一次性校验。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class CaptchaService {

    /** store 不可变字段。 */
    private final CaptchaStore store;
    /** generators 不可变字段。 */
    private final Map<ExtrasWebProperties.Captcha.CaptchaType, CaptchaGenerator> generators;
    /** props 不可变字段。 */
    private final ExtrasWebProperties.Captcha props;

    /**
     * 构造实例。
     * @param store 参数 store
     * @param props 参数 props
     */
    public CaptchaService(CaptchaStore store, List<CaptchaGenerator> generators,
                          ExtrasWebProperties.Captcha props) {
        this.store = store;
        this.props = props;
        this.generators = generators.stream()
                .collect(Collectors.toMap(CaptchaGenerator::type, Function.identity(), (a, b) -> a));
    }

    /**
     * 生成验证码。
     *
     * @return 验证码（含标识与展示内容）
     */
    public Captcha generate() {
        CaptchaGenerator generator = generators.get(props.getType());
        if (generator == null) {
            throw new cn.jowen.framework.extras.common.exception.ExtrasException(
                    "未注册的验证码类型: " + props.getType());
        }
        Captcha captcha = generator.generate(props.getExpireSeconds());
        store.save(captcha);
        return captcha;
    }

    /**
     * 生成短信验证码（需指定接收手机号）。
     *
     * @param phone 接收手机号
     * @return 验证码
     */
    public Captcha generateSms(String phone) {
        CaptchaGenerator generator = generators.get(ExtrasWebProperties.Captcha.CaptchaType.SMS);
        if (!(generator instanceof SmsCaptchaGenerator sms)) {
            throw new cn.jowen.framework.extras.common.exception.ExtrasException(
                    "未注册短信验证码生成器，请注入 SmsCaptchaGenerator");
        }
        Captcha captcha = sms.generate(phone, props.getExpireSeconds());
        store.save(captcha);
        return captcha;
    }

    /**
     * 校验验证码，校验通过后立即失效（一次性）。
     *
     * @param id   验证码标识
     * @param code 用户输入的答案
     * @return 是否校验通过
     */
    public boolean validate(String id, String code) {
        Captcha captcha = store.get(id);
        if (captcha == null) {
            return false;
        }
        if (captcha.expired()) {
            store.remove(id);
            return false;
        }
        if (!captcha.code().equalsIgnoreCase(code == null ? "" : code.trim())) {
            return false;
        }
        store.remove(id);
        return true;
    }

    /**
     * 校验滑块验证码位置（在 {@link SliderCaptchaGenerator#TOLERANCE} 容差内）。
     *
     * @param id      验证码标识
     * @param actualX 用户拖动的横坐标
     * @return 是否校验通过
     */
    public boolean validateSlider(String id, int actualX) {
        Captcha captcha = store.get(id);
        if (captcha == null || captcha.expired()) {
            if (captcha != null) {
                store.remove(id);
            }
            return false;
        }
        if (!SliderCaptchaGenerator.validatePosition(captcha.code(), actualX)) {
            return false;
        }
        store.remove(id);
        return true;
    }
}
