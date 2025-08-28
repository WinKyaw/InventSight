package com.pos.inventsight.dto;

import com.pos.inventsight.model.sql.UserSettings;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class UserSettingsRequest {
    
    // Display & UI Settings
    private String theme;
    
    @Size(max = 10, message = "Language code cannot exceed 10 characters")
    private String language;
    
    @Size(max = 50, message = "Timezone cannot exceed 50 characters")
    private String timezone;
    
    @Size(max = 20, message = "Date format cannot exceed 20 characters")
    private String dateFormat;
    
    @Size(max = 20, message = "Time format cannot exceed 20 characters")
    private String timeFormat;
    
    @Size(max = 10, message = "Currency code cannot exceed 10 characters")
    private String currency;
    
    // Notification Settings
    private Boolean emailNotifications;
    private Boolean pushNotifications;
    private Boolean smsNotifications;
    private String notificationFrequency;
    
    // Inventory Settings
    @Min(value = 0, message = "Low stock threshold cannot be negative")
    @Max(value = 10000, message = "Low stock threshold cannot exceed 10000")
    private Integer lowStockThreshold;
    
    private Boolean autoReorder;
    private Long defaultSupplierId;
    
    // Calendar Settings
    private String calendarView;
    
    @Min(value = 0, message = "Working hours start must be between 0 and 23")
    @Max(value = 23, message = "Working hours start must be between 0 and 23")
    private Integer workingHoursStart;
    
    @Min(value = 0, message = "Working hours end must be between 0 and 23")
    @Max(value = 23, message = "Working hours end must be between 0 and 23")
    private Integer workingHoursEnd;
    
    private Boolean weekendIncluded;
    
    // Security Settings
    private Boolean twoFactorEnabled;
    
    @Min(value = 15, message = "Session timeout must be at least 15 minutes")
    @Max(value = 1440, message = "Session timeout cannot exceed 1440 minutes (24 hours)")
    private Integer sessionTimeoutMinutes;
    
    private Boolean passwordChangeRequired;
    
    // Privacy Settings
    private String profileVisibility;
    private Boolean activityTracking;
    private Boolean dataSharing;
    
    // Constructors
    public UserSettingsRequest() {}
    
    // Getters and Setters
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    
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
    
    public String getNotificationFrequency() { return notificationFrequency; }
    public void setNotificationFrequency(String notificationFrequency) { this.notificationFrequency = notificationFrequency; }
    
    public Integer getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(Integer lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }
    
    public Boolean getAutoReorder() { return autoReorder; }
    public void setAutoReorder(Boolean autoReorder) { this.autoReorder = autoReorder; }
    
    public Long getDefaultSupplierId() { return defaultSupplierId; }
    public void setDefaultSupplierId(Long defaultSupplierId) { this.defaultSupplierId = defaultSupplierId; }
    
    public String getCalendarView() { return calendarView; }
    public void setCalendarView(String calendarView) { this.calendarView = calendarView; }
    
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
    
    public String getProfileVisibility() { return profileVisibility; }
    public void setProfileVisibility(String profileVisibility) { this.profileVisibility = profileVisibility; }
    
    public Boolean getActivityTracking() { return activityTracking; }
    public void setActivityTracking(Boolean activityTracking) { this.activityTracking = activityTracking; }
    
    public Boolean getDataSharing() { return dataSharing; }
    public void setDataSharing(Boolean dataSharing) { this.dataSharing = dataSharing; }
}