package demo.ms.message.stream;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.messaging.SubscribableChannel;

public interface LoggerEventSource {
    public static final String MESSAGE_QUEUE = "MESSAGE_QUEUE";

    @Input(MESSAGE_QUEUE)
    SubscribableChannel input();
}
