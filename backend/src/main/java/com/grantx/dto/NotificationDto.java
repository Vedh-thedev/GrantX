package com.grantx.dto;

import lombok.Data;

public class NotificationDto {

    @Data
    public static class NotificationResponse {
        private Long id;
        private String title;
        private String message;
        private String notificationType;
        private boolean isRead;
        private String createdAt;
        private Long proposalId;
        private String proposalTitle;
    }
}
