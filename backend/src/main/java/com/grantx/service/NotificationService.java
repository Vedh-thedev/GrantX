package com.grantx.service;

import com.grantx.dto.NotificationDto;
import com.grantx.entity.Notification;
import com.grantx.entity.Proposal;
import com.grantx.entity.User;
import com.grantx.exception.ResourceNotFoundException;
import com.grantx.exception.UnauthorizedException;
import com.grantx.repository.NotificationRepository;
import com.grantx.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createNotification(User user, Proposal proposal, String title, String message, String type) {
        Notification notification = Notification.builder()
                .user(user)
                .proposal(proposal)
                .title(title)
                .message(message)
                .notificationType(type)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
    }

    public List<NotificationDto.NotificationResponse> getUserNotifications(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return notificationRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long notificationId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Cannot access this notification");
        }
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<Notification> unread = notificationRepository.findByUserAndIsReadOrderByCreatedAtDesc(user, false);
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
    }

    public long getUnreadCount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return notificationRepository.countByUserAndIsRead(user, false);
    }

    private NotificationDto.NotificationResponse mapToResponse(Notification n) {
        NotificationDto.NotificationResponse r = new NotificationDto.NotificationResponse();
        r.setId(n.getId());
        r.setTitle(n.getTitle());
        r.setMessage(n.getMessage());
        r.setNotificationType(n.getNotificationType());
        r.setRead(n.getIsRead());
        r.setCreatedAt(n.getCreatedAt() != null ? n.getCreatedAt().toString() : null);
        r.setProposalId(n.getProposal() != null ? n.getProposal().getId() : null);
        r.setProposalTitle(n.getProposal() != null ? n.getProposal().getTitle() : null);
        return r;
    }
}
