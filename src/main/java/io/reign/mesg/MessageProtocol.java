package io.reign.mesg;

/**
 * 
 * @author ypai
 * 
 */
public interface MessageProtocol {

    /**
     * 
     * @param asciiRequest
     *            UTF-8 assumed
     * @return
     */
    public RequestMessage fromTextRequest(String textRequest);

    /**
     * 
     * @param bytes
     * @return
     */
    public RequestMessage fromBinaryRequest(byte[] bytes);

    /**
     * 
     * @param textResponse
     * @return
     */
    public ResponseMessage fromTextResponse(String textResponse);

    /**
     * 
     * @param bytes
     * @return
     */
    public ResponseMessage fromBinaryResponse(byte[] bytes);

    /**
     * 
     * @param responseMessage
     * @return UTF-8 String
     */
    public String toTextResponse(ResponseMessage responseMessage);

    /**
     * 
     * @param responseMessage
     * @return
     */
    public byte[] toBinaryResponse(ResponseMessage responseMessage);

    /**
     * 
     * @param requestMessage
     * @return
     */
    public String toTextRequest(RequestMessage requestMessage);

    /**
     * 
     * @param requestMessage
     * @return
     */
    public byte[] toBinaryRequest(RequestMessage requestMessage);

    public String toTextEvent(EventMessage eventMessage);

    public byte[] toBinaryEvent(EventMessage eventMessage);

}
