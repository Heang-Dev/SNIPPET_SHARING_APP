package group.eleven.snippet_sharing_app.data.model;

import com.google.gson.annotations.SerializedName;

import group.eleven.snippet_sharing_app.api.ApiClient;

/**
 * User model representing a user in the system
 * Fields match the backend User model from Laravel
 */
public class User {
    @SerializedName("id")
    private String id;

    @SerializedName("username")
    private String username;

    @SerializedName("email")
    private String email;

    @SerializedName("full_name")
    private String fullName;

    @SerializedName("bio")
    private String bio;

    @SerializedName("avatar_url")
    private String avatarUrl;

    @SerializedName("avatar")
    private String avatar;

    @SerializedName("location")
    private String location;

    @SerializedName("latitude")
    private Double latitude;

    @SerializedName("longitude")
    private Double longitude;

    @SerializedName("company")
    private String company;

    @SerializedName("website_url")
    private String websiteUrl;

    @SerializedName("github_url")
    private String githubUrl;

    @SerializedName("twitter_url")
    private String twitterUrl;

    @SerializedName("is_admin")
    private boolean isAdmin;

    @SerializedName("is_active")
    private boolean isActive;

    @SerializedName("profile_visibility")
    private String profileVisibility;

    @SerializedName("show_email")
    private boolean showEmail;

    @SerializedName("show_activity")
    private boolean showActivity;

    @SerializedName("default_snippet_privacy")
    private String defaultSnippetPrivacy;

    @SerializedName("theme_preference")
    private String themePreference;

    @SerializedName("snippets_count")
    private int snippetsCount;

    @SerializedName("followers_count")
    private int followersCount;

    @SerializedName("following_count")
    private int followingCount;

    @SerializedName("likes_received_count")
    private int likesReceivedCount;

    @SerializedName("social_provider")
    private String socialProvider;

    @SerializedName("social_id")
    private String socialId;

    @SerializedName("email_verified_at")
    private String emailVerifiedAt;

    @SerializedName("last_login_at")
    private String lastLoginAt;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    // Follow state (returned in followers/following list responses)
    @SerializedName("is_following")
    private boolean isFollowing;

    // Getters
    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getBio() {
        return bio;
    }

    public String getAvatarUrl() {
        // Convert relative path to full URL for image loading
        return ApiClient.getFullStorageUrl(avatarUrl);
    }

    /**
     * Get the raw avatar URL without conversion (for saving to backend)
     */
    public String getRawAvatarUrl() {
        return avatarUrl;
    }

    public String getAvatar() {
        return avatar;
    }

    public String getLocation() {
        return location;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public String getCompany() {
        return company;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public String getTwitterUrl() {
        return twitterUrl;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public boolean isActive() {
        return isActive;
    }

    public String getProfileVisibility() {
        return profileVisibility;
    }

    public boolean isShowEmail() {
        return showEmail;
    }

    public boolean isShowActivity() {
        return showActivity;
    }

    public String getDefaultSnippetPrivacy() {
        return defaultSnippetPrivacy;
    }

    public String getThemePreference() {
        return themePreference;
    }

    public int getSnippetsCount() {
        return snippetsCount;
    }

    public int getFollowersCount() {
        return followersCount;
    }

    public int getFollowingCount() {
        return followingCount;
    }

    public int getLikesReceivedCount() {
        return likesReceivedCount;
    }

    public String getSocialProvider() {
        return socialProvider;
    }

    public String getSocialId() {
        return socialId;
    }

    public String getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public String getLastLoginAt() {
        return lastLoginAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null && !emailVerifiedAt.isEmpty();
    }

    public boolean isFollowing() {
        return isFollowing;
    }

    public void setFollowing(boolean following) {
        isFollowing = following;
    }

    /**
     * Get the best available avatar URL as a full URL
     * Falls back to avatar field if avatar_url is null
     * Converts relative paths to full URLs using the storage base URL
     */
    public String getEffectiveAvatarUrl() {
        String url = avatarUrl;
        if (url == null || url.isEmpty()) {
            url = avatar;
        }
        // Convert relative path to full URL
        return ApiClient.getFullStorageUrl(url);
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }

    public void setTwitterUrl(String twitterUrl) {
        this.twitterUrl = twitterUrl;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public void setProfileVisibility(String profileVisibility) {
        this.profileVisibility = profileVisibility;
    }

    public void setShowEmail(boolean showEmail) {
        this.showEmail = showEmail;
    }

    public void setShowActivity(boolean showActivity) {
        this.showActivity = showActivity;
    }

    public void setDefaultSnippetPrivacy(String defaultSnippetPrivacy) {
        this.defaultSnippetPrivacy = defaultSnippetPrivacy;
    }

    public void setThemePreference(String themePreference) {
        this.themePreference = themePreference;
    }

    public void setSnippetsCount(int snippetsCount) {
        this.snippetsCount = snippetsCount;
    }

    public void setFollowersCount(int followersCount) {
        this.followersCount = followersCount;
    }

    public void setFollowingCount(int followingCount) {
        this.followingCount = followingCount;
    }

    public void setSocialProvider(String socialProvider) {
        this.socialProvider = socialProvider;
    }

    public void setSocialId(String socialId) {
        this.socialId = socialId;
    }

    public void setEmailVerifiedAt(String emailVerifiedAt) {
        this.emailVerifiedAt = emailVerifiedAt;
    }

    public void setLastLoginAt(String lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", snippetsCount=" + snippetsCount +
                ", followersCount=" + followersCount +
                ", followingCount=" + followingCount +
                '}';
    }
}
