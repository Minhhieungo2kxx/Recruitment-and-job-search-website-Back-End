package com.webjob.application.Models.Response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ConversationDTO {
    private Long id;
    private Long user1Id;
    private Long user2Id;
    private Long lastMessageId;
    private Instant createdAt;
    private Instant updatedAt;
}
