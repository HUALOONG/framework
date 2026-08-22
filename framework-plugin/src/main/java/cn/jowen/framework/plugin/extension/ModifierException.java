package cn.jowen.framework.plugin.extension;

import org.jspecify.annotations.NullMarked;

/** Marker to indicate abstract modifier check is not available via standard API in this context. */
@NullMarked
final class ModifierException extends RuntimeException {
    ModifierException(String msg) { super(msg); }
}
