package io.reign.mesg;

import io.reign.util.JacksonUtil;

import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author ypai
 * 
 */
@JsonPropertyOrder({ "id", "status" })
public class SimpleResponseMessage extends AbstractMessage implements ResponseMessage {

    private static final Logger logger = LoggerFactory.getLogger(SimpleResponseMessage.class);

    private ResponseStatus status = ResponseStatus.OK;

    private String comment = null;

    public SimpleResponseMessage() {
    }

    public SimpleResponseMessage(ResponseStatus status) {
        setStatus(status);
    }

    public SimpleResponseMessage(ResponseStatus status, Integer id) {
        setStatus(status);
        setId(id);
    }

    public SimpleResponseMessage(ResponseStatus status, Object body) {
        setStatus(status);
        setBody(body);
    }

    public SimpleResponseMessage(ResponseStatus status, Object body, String comment) {
        setStatus(status);
        setBody(body);
        setComment(comment);
    }

    public SimpleResponseMessage(ResponseStatus status, Integer id, Object body, String comment) {
        setStatus(status);
        setId(id);
        setBody(body);
        setComment(comment);
    }

    @Override
    public ResponseStatus getStatus() {
        return status;
    }

    @Override
    public String getComment() {
        return comment;
    }

    @Override
    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public ResponseMessage setStatus(ResponseStatus status) {
        this.status = status;
        return this;
    }

    @Override
    public ResponseMessage setStatus(ResponseStatus status, String comment) {
        this.status = status;
        this.comment = comment;
        return this;
    }

    @Override
    public String toString() {
        try {
            return JacksonUtil.getObjectMapper().writeValueAsString(this);
        } catch (Exception e) {
            logger.error("" + e, e);
            return super.toString();
        }
    }

}
