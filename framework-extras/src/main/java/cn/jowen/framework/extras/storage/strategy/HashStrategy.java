package cn.jowen.framework.extras.storage.strategy;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * 哈希分桶命名策略：基于文件名（含随机盐）计算 SHA-256 后分桶，生成
 * {@code <bucket>/<hex>_<原名>} 形式对象名。
 *
 * <p>说明：命名阶段仅能获取文件名，故以文件名 + 随机盐哈希作为内容寻址的近似；
 * 若需真实内容寻址，应在上传后由调用方回写。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public final class HashStrategy implements ObjectNameStrategy {

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    @Override
    public String generate(@Nullable String originalFilename) {
        String base = DatePathStrategy.basename(originalFilename);
        String salted = base + "|" + UUID.randomUUID();
        String hex = sha256Hex(salted);
        String bucket = hex.substring(0, 2);
        return bucket + "/" + hex + "_" + base;
    }
}
