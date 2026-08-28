package cn.jowen.framework.i18n.reload;

import cn.jowen.framework.i18n.api.ReloadableMessageSource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link RedisSubscriptionWatcher} 测试。
 */
class RedisSubscriptionWatcherTest {

    private final RedissonClient client = mock(RedissonClient.class);
    private final RTopic topic = mock(RTopic.class);
    private final ReloadableMessageSource target = mock(ReloadableMessageSource.class);
    private final AtomicInteger reloadCount = new AtomicInteger();
    private final ResourceReloader reloader = new ResourceReloader();
    private final String channel = "i18n:changed";

    RedisSubscriptionWatcherTest() {
        doAnswer(invocation -> {
            reloadCount.incrementAndGet();
            return null;
        }).when(target).reload();
    }

    @Test
    void start_subscribesTopic() {
        when(client.getTopic(channel)).thenReturn(topic);
        when(topic.addListener(eq(String.class), any())).thenReturn(1);
        RedisSubscriptionWatcher watcher = new RedisSubscriptionWatcher(client, channel, target, reloader);

        watcher.start();

        assertThat(watcher.isRunning()).isTrue();
        verify(client).getTopic(channel);
        verify(topic).addListener(eq(String.class), any());
    }

    @Test
    void start_isIdempotent() {
        when(client.getTopic(channel)).thenReturn(topic);
        when(topic.addListener(eq(String.class), any())).thenReturn(1);
        RedisSubscriptionWatcher watcher = new RedisSubscriptionWatcher(client, channel, target, reloader);

        watcher.start();
        watcher.start();

        assertThat(watcher.isRunning()).isTrue();
        verify(topic, times(1)).addListener(eq(String.class), any());
    }

    @Test
    void stop_unsubscribesListener() {
        when(client.getTopic(channel)).thenReturn(topic);
        when(topic.addListener(eq(String.class), any())).thenReturn(42);
        RedisSubscriptionWatcher watcher = new RedisSubscriptionWatcher(client, channel, target, reloader);

        watcher.start();
        watcher.stop();

        assertThat(watcher.isRunning()).isFalse();
        verify(topic).removeListener(42);
    }

    @Test
    void stop_isIdempotent() {
        when(client.getTopic(channel)).thenReturn(topic);
        when(topic.addListener(eq(String.class), any())).thenReturn(42);
        RedisSubscriptionWatcher watcher = new RedisSubscriptionWatcher(client, channel, target, reloader);

        watcher.start();
        watcher.stop();
        watcher.stop();

        verify(topic, times(1)).removeListener(42);
    }

    @Test
    void messageReceived_triggersReload() {
        when(client.getTopic(channel)).thenReturn(topic);
        when(topic.addListener(eq(String.class), any())).thenReturn(1);
        RedisSubscriptionWatcher watcher = new RedisSubscriptionWatcher(client, channel, target, reloader);
        watcher.start();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<MessageListener<String>> captor = ArgumentCaptor.forClass(MessageListener.class);
        verify(topic).addListener(eq(String.class), captor.capture());

        captor.getValue().onMessage(channel, "changed");

        assertThat(reloadCount.get()).isEqualTo(1);
        watcher.stop();
    }
}
