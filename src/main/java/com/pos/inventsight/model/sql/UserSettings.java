package com.pos.inventsight.model.sql;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_settings")
public class UserSettings {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;
    
    // Display & UI Settings
    @Column(name = "theme")
    @Enumerated(EnumType.STRING)
    private Theme theme = Theme.LIGHT;
    
    @Size(max = 10)
    private String language = "en";
    
    @Size(max = 50)
    private String timezone = "UTC";
    
    @Column(name = "date_format")
    @Size(max = 20)
    private String dateFormat = "yyyy-MM-dd";
    
    @Column(name = "time_format")
    @Size(max = 20)
    private String timeFormat = "HH:mm:ss";
    
    @Column(name = "currency")
    @Size(max = 10)
    private String currency = "USD";
    
    // Notification Settings
    @Column(name = "email_notifications")
    private Boolean emailNotifications = true;
    
    @Column(name = "push_notifications")
    private Boolean pushNotifications = true;
    
    @Column(name = "sms_notifications")
    private Boolean smsNotifications = false;
    
    @Column(name = "notification_frequency")
    @Enumerated(EnumType.STRING)
    private NotificationFrequency notificationFrequency = NotificationFrequency.IMMEDIATE;
    
    // Inventory Settings
    @Column(name = "low_stock_threshold")
    private Integer lowStockThreshold = 10;
    
    @Column(name = "auto_reorder")
    private Boolean autoReorder = false;
    
    @Column(name = "default_supplier_id")
    private Long defaultSupplierId;
    
    // Calendar Settings
    @Column(name = "calendar_view")
    @Enumerated(EnumType.STRING)
    private CalendarView calendarView = CalendarView.WEEK;
    
    @Column(name = "working_hours_start")
    private Integer workingHoursStart = 9;
    
    @Column(name = "working_hours_end")
    private Integer workingHoursEnd = 17;
    
    @Column(name = "weekend_included")
    private Boolean weekendIncluded = false;
    
    // Security Settings
    @Column(name = "two_factor_enabled")
    private Boolean twoFactorEnabled = false;
    
    @Column(name = "session_timeout_minutes")
    private Integer sessionTimeoutMinutes = 480; // 8 hours
    
    @Column(name = "password_change_required")
    private Boolean passwordChangeRequired = false;
    
    // Privacy Settings
    @Column(name = "profile_visibility")
    @Enumerated(EnumType.STRING)
    private ProfileVisibility profileVisibility = ProfileVisibility.TEAM;
    
    @Column(name = "activity_tracking")
    private Boolean activityTracking = true;
    
    @Column(name = "data_sharing")
    private Boolean dataSharing = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // Constructors
    public UserSettings() {}
    
    public UserSettings(User user) {
        this.user = user;
    }
    
    // Enums
    public enum Theme {
        LIGHT, DARK, AUTO
    }
    
    public enum NotificationFrequency {
        IMMEDIATE, HOURLY, DAILY, WEEKLY, NEVER
    }
    
    public enum CalendarView {
        DAY, WEEK, MONTH, AGENDA
    }
    
    public enum ProfileVisibility {
        PUBLIC, TEAM, PRIVATE
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public Theme getTheme() { return theme; }
    public void setTheme(Theme theme) { this.theme = theme; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    
    public String getDateFormat() { return dateFormat; }
    public void setDateFormat(String dateFormat) { this.dateFormat = dateFormat; }
    
    public String getTimeFormat() { return timeFormat; }
    public void setTimeFormat(String timeFormat) { this.timeFormat = timeFormat; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public Boolean getEmailNotifications() { return emailNotifications; }
    public void setEmailNotifications(Boolean emailNotifications) { this.emailNotifications = emailNotifications; }
    
    public Boolean getPushNotifications() { return pushNotifications; }
    public void setPushNotifications(Boolean pushNotifications) { this.pushNotifications = pushNotifications; }
    
    public Boolean getSmsNotifications() { return smsNotifications; }
    public void setSmsNotifications(Boolean smsNotifications) { this.smsNotifications = smsNotifications; }
    
    public NotificationFrequency getNotificationFrequency() { return notificationFrequency; }
    public void setNotificationFrequency(NotificationFrequency notificationFrequency) { this.notificationFrequency = notificationFrequency; }
    
    public Integer getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(Integer lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }
    
    public Boolean getAutoReorder() { return autoReorder; }
    public void setAutoReorder(Boolean autoReorder) { this.autoReorder = autoReorder; }
    
    public Long getDefaultSupplierId() { return defaultSupplierId; }
    public void setDefaultSupplierId(Long defaultSupplierId) { this.defaultSupplierId = defaultSupplierId; }
    
    public CalendarView getCalendarView() { return calendarView; }
    public void setCalendarView(CalendarView calendarView) { this.calendarView = calendarView; }
    
    public Integer getWorkingHoursStart() { return workingHoursStart; }
    public void setWorkingHoursStart(Integer workingHoursStart) { this.workingHoursStart = workingHoursStart; }
    
    public Integer getWorkingHoursEnd() { return workingHoursEnd; }
    public void setWorkingHoursEnd(Integer workingHoursEnd) { this.workingHoursEnd = workingHoursEnd; }
    
    public Boolean getWeekendIncluded() { return weekendIncluded; }
    public void setWeekendIncluded(Boolean weekendIncluded) { this.weekendIncluded = weekendIncluded; }
    
    public Boolean getTwoFactorEnabled() { return twoFactorEnabled; }
    public void setTwoFactorEnabled(Boolean twoFactorEnabled) { this.twoFactorEnabled = twoFactorEnabled; }
    
    public Integer getSessionTimeoutMinutes() { return sessionTimeoutMinutes; }
    public void setSessionTimeoutMinutes(Integer sessionTimeoutMinutes) { this.sessionTimeoutMinutes = sessionTimeoutMinutes; }
    
    public Boolean getPasswordChangeRequired() { return passwordChangeRequired; }
    public void setPasswordChangeRequired(Boolean passwordChangeRequired) { this.passwordChangeRequired = passwordChangeRequired; }
    
    public ProfileVisibility getProfileVisibility() { return profileVisibility; }
    public void setProfileVisibility(ProfileVisibility profileVisibility) { this.profileVisibility = profileVisibility; }
    
    public Boolean getActivityTracking() { return activityTracking; }
    public void setActivityTracking(Boolean activityTracking) { this.activityTracking = activityTracking; }
    
    public Boolean getDataSharing() { return dataSharing; }
    public void setDataSharing(Boolean dataSharing) { this.dataSharing = dataSharing; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}