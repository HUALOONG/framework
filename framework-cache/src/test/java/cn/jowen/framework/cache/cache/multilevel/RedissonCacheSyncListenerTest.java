package cn.jowen.framework.cache.cache.multilevel;

import cn.jowen.framework.cache.api.Cache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedissonCacheSyncListenerTest {

    @Mock RedissonClient redissonClient;
    @Mock RTopic topic;
    @Mock Cache<Object, Object> localCache;
    @Captor ArgumentCaptor<MessageListener<CacheSyncMessage>> listenerCaptor;

    private RedissonCacheSyncListener newListener() {
        when(redissonClient.getTopic(anyString())).thenReturn(topic);
        return new RedissonCacheSyncListener(localCache, redissonClient, "topic");
    }

    @Test
    void subscribesOnConstruction() {
        newListener();
        verify(topic).addListener(eq(CacheSyncMessage.class), any(MessageListener.class));
    }

    @Test
    void appliesRemotePutToLocalCache() {
        RedissonCacheSyncListener listener = newListener();
        verify(topic).addListener(eq(CacheSyncMessage.class), listenerCaptor.capture());
        listenerCaptor.getValue().onMessage("topic", new CacheSyncMessage(CacheSyncMessage.Type.PUT, "k", "v"));
        verify(localCache).put("k", "v");
    }

    @Test
    void appliesRemoteEvictToLocalCache() {
        RedissonCacheSyncListener listener = newListener();
        verify(topic).addListener(eq(CacheSyncMessage.class), listenerCaptor.capture());
        listenerCaptor.getValue().onMessage("topic", new CacheSyncMessage(CacheSyncMessage.Type.EVICT, "k", null));
        verify(localCache).evict("k");
    }

    @Test
    void publishPutSendsMessage() {
        RedissonCacheSyncListener listener = newListener();
        listener.publishPut("k", "v");
        verify(topic).publish(new CacheSyncMessage(CacheSyncMessage.Type.PUT, "k", "v"));
    }

    @Test
    void publishEvictSendsMessage() {
        RedissonCacheSyncListener listener = newListener();
        listener.publishEvict("k");
        verify(topic).publish(new CacheSyncMessage(CacheSyncMessage.Type.EVICT, "k", null));
    }

    @Test
    void messageRecordEquality() {
        assertEquals(new CacheSyncMessage(CacheSyncMessage.Type.PUT, "k", "v"),
                new CacheSyncMessage(CacheSyncMessage.Type.PUT, "k", "v"));
    }
}
