package demo.ms.message.config;

import io.netty.util.internal.ConcurrentSet;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

public class StompHandlerDecoratorFactory implements WebSocketHandlerDecoratorFactory {

    private RedisTemplate<String,String> redisTemplate;

    private ConcurrentSet<String> onlineUsers = new ConcurrentSet<>();

    public ConcurrentSet<String> getOnlineUsers() {
        return onlineUsers;
    }

    public StompHandlerDecoratorFactory(RedisTemplate<String,String> redisTemplate){
        this.redisTemplate = redisTemplate;
    }

    @Override
    public WebSocketHandler decorate(final WebSocketHandler handler) {
        return new WebSocketHandlerDecorator(handler) {
            @Override
            public void afterConnectionEstablished(final WebSocketSession session) throws Exception {
                // 客户端与服务器端建立连接后，此处记录谁上线了
                String userId = session.getPrincipal().getName();
                onlineUsers.add(userId);
                System.out.println("online: " + userId);
                redisTemplate.opsForSet().add("STOMP_ONLINE_USER",userId);
                super.afterConnectionEstablished(session);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                // 客户端与服务器端断开连接后，此处记录谁下线了
                String userId = session.getPrincipal().getName();
                onlineUsers.remove(userId);
                System.out.println("offline: " + userId);
                redisTemplate.opsForSet().remove("STOMP_ONLINE_USER",userId);
                super.afterConnectionClosed(session, closeStatus);
            }
        };
    }
}
