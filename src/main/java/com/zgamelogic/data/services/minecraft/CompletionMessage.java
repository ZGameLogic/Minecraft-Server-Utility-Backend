package com.zgamelogic.data.services.minecraft;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompletionMessage {
    private String message;
    private boolean success;
    private Object data;

    public static CompletionMessage fail(String message){
        return new CompletionMessage(message, false, null);
    }

    public static CompletionMessage success(String message){
        return new CompletionMessage(message, true, null);
    }

    public static CompletionMessage fail(String message, Object data){
        return new CompletionMessage(message, false, data);
    }

    public static CompletionMessage success(String message, Object data){
        return new CompletionMessage(message, true, data);
    }
}
