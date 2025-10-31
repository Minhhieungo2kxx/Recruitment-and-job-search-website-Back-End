package com.webjob.application.Dto.Request.Websockets;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageUpdateDTO {
    @NotNull(message = "ID tin nhắn không được để trống")
    private Long messageId;

    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    private String content;
}
