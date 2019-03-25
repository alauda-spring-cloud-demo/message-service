package demo.ms.message.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.RsaVerifier;
import org.springframework.security.oauth2.common.util.JsonParserFactory;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Slf4j
@EnableWebSocketMessageBroker
@Configuration
public class WebSocketConfig extends AbstractWebSocketMessageBrokerConfigurer {

    @Autowired
    JwtAccessTokenConverter jwtAccessTokenConverter;

    @Autowired
    RedisTemplate<String,String> redisTemplate;

    @Value("${rabbit.host}")
    String rabbitHost;

    @Value("${rabbit.web-stomp-port}")
    int rabbitPort;

    @Value("${rabbit.username}")
    String rabbitUsername;

    @Value("${rabbit.password}")
    String rabbitPassword;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
                .addEndpoint("/stomp")
                .setAllowedOrigins("*")
                .addInterceptors(handshakeInterceptor())
                .setHandshakeHandler(handshakeHandler())
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry){
        registry
                .setApplicationDestinationPrefixes("/app")
                .enableStompBrokerRelay("/exchange","/topic","/queue","/amq/queue")
                .setVirtualHost("/")
                .setRelayHost(rabbitHost)
//                .setRelayPort(rabbitPort)
                .setClientLogin(rabbitUsername)
                .setClientPasscode(rabbitPassword)
                .setSystemLogin(rabbitUsername)
                .setSystemPasscode(rabbitPassword)
                .setSystemHeartbeatSendInterval(5000)
                .setSystemHeartbeatReceiveInterval(4000);
    }

    @Bean
    public HandshakeInterceptor handshakeInterceptor(){
        return new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest serverHttpRequest, ServerHttpResponse
                    serverHttpResponse, WebSocketHandler webSocketHandler, Map<String, Object> map){
                ServletServerHttpRequest req = (ServletServerHttpRequest)serverHttpRequest;
                String token = req.getServletRequest().getParameter("token");

                Principal principal = retrieveUserAuthToken(token);

                if(principal==null){
                    return false;
                }

                map.put("user",principal);
                return true;
            }

            @Override
            public void afterHandshake(ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse,
                                       WebSocketHandler webSocketHandler, Exception e) {
            }
        };
    }

    @Bean
    public HandshakeHandler handshakeHandler(){
        return new DefaultHandshakeHandler(){
            @Override
            protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {

                return (Principal)attributes.get("user");
            }
        };
    }

    private Principal retrieveUserAuthToken(String token){

        RsaVerifier rsaVerifier = new RsaVerifier(this.jwtAccessTokenConverter.getKey().get("value"));
        Jwt jwt = JwtHelper.decodeAndVerify(token,rsaVerifier);

        String content = jwt.getClaims();
        Map<String, Object> claims = JsonParserFactory.create().parseMap(content);

        return () -> String.valueOf(claims.get("id"));
    }

    @Bean
    StompHandlerDecoratorFactory stompHandlerDecoratorFactory(){
        return new StompHandlerDecoratorFactory(redisTemplate);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.addDecoratorFactory(stompHandlerDecoratorFactory());
        super.configureWebSocketTransport(registration);
    }
}
