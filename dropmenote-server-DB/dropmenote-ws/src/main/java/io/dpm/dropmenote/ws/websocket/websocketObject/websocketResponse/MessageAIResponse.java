package io.dpm.dropmenote.ws.websocket.websocketObject.websocketResponse;

public class MessageAIResponse extends AbstractResponse {

    private String data;

    public MessageAIResponse(String data) {
        super(WebsocketResponseTypeEnum.MESSAGE_AI);
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
