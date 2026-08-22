/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.captcha.generator;

import cn.jowen.framework.extras.captcha.CaptchaProperties;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.security.SecureRandom;
import java.util.Random;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 算术验证码生成器：随机生成加法/减法算式，答案与算式自洽。
 *
 * <p>减法保证非负（取两数较大者减较小者）；算式文本渲染于图片，答案单独返回供校验。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public final class ArithmeticCaptchaGenerator implements CaptchaGenerator {

    /** 算式构造结果（供单测验证答案与算式自洽）。 */
    public record ArithmeticProblem(int a, int b, boolean add, int answer, String expression) {
    }

    private final CaptchaProperties properties;
    private final Random random = new SecureRandom();

    public ArithmeticCaptchaGenerator() {
        this(new CaptchaProperties());
    }

    public ArithmeticCaptchaGenerator(@Nullable CaptchaProperties properties) {
        this.properties = properties == null ? new CaptchaProperties() : properties;
    }

    /**
     * 构造一道随机算式（不渲染图片，便于校验答案自洽性）。
     *
     * @return 算式问题与答案
     */
    public ArithmeticProblem buildProblem() {
        int a = 1 + random.nextInt(10);
        int b = 1 + random.nextInt(10);
        boolean add = random.nextBoolean();
        int answer;
        String expression;
        if (add) {
            answer = a + b;
            expression = a + " + " + b + " = ?";
        } else {
            int x = Math.max(a, b);
            int y = Math.min(a, b);
            answer = x - y;
            expression = x + " - " + y + " = ?";
        }
        return new ArithmeticProblem(a, b, add, answer, expression);
    }

    @Override
    public CaptchaImage generate() {
        ArithmeticProblem problem = buildProblem();
        int width = properties.getWidth();
        int height = properties.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.setColor(Color.BLACK);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(12, height - 10)));
            g.drawString(problem.expression(), 6, height / 2 + 6);
            return new CaptchaImage(image, String.valueOf(problem.answer()));
        } finally {
            g.dispose();
        }
    }
}
