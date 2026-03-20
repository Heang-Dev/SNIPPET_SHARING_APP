package group.eleven.snippet_sharing_app.data.model;

/**
 * Model class for snippet cards with author info for social feed
 */
public class SnippetCard {
    private String id;
    private String slug;
    private String title;
    private String description;
    private String languageBadge;
    private String updatedTime;
    private String codePreview;
    private String[] tags;
    private int languageColor;

    // Author info for social feed
    private String authorName;
    private String authorAvatar;
    private String authorUsername;

    // Social stats
    private int likesCount;
    private int commentsCount;
    private boolean isLiked;
    private String visibility; // "public", "private", "team"

    // Legacy constructor for backwards compatibility
    public SnippetCard(String title, String languageBadge, String updatedTime,
                      String codePreview, String[] tags, int languageColor) {
        this.title = title;
        this.languageBadge = languageBadge;
        this.updatedTime = updatedTime;
        this.codePreview = codePreview;
        this.tags = tags;
        this.languageColor = languageColor;
        this.visibility = "public";
    }

    // Full constructor with social data
    public SnippetCard(String id, String title, String description, String languageBadge,
                      String updatedTime, String codePreview, String[] tags, int languageColor,
                      String authorName, String authorAvatar, String authorUsername,
                      int likesCount, int commentsCount, boolean isLiked, String visibility) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.languageBadge = languageBadge;
        this.updatedTime = updatedTime;
        this.codePreview = codePreview;
        this.tags = tags;
        this.languageColor = languageColor;
        this.authorName = authorName;
        this.authorAvatar = authorAvatar;
        this.authorUsername = authorUsername;
        this.likesCount = likesCount;
        this.commentsCount = commentsCount;
        this.isLiked = isLiked;
        this.visibility = visibility;
    }

    public String getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getLanguageBadge() {
        return languageBadge;
    }

    public String getUpdatedTime() {
        return updatedTime;
    }

    public String getCodePreview() {
        return codePreview;
    }

    public String[] getTags() {
        return tags;
    }

    public int getLanguageColor() {
        return languageColor;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getAuthorAvatar() {
        return authorAvatar;
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public int getLikesCount() {
        return likesCount;
    }

    public int getCommentsCount() {
        return commentsCount;
    }

    public boolean isLiked() {
        return isLiked;
    }

    public void setLiked(boolean liked) {
        isLiked = liked;
    }

    public void setLikesCount(int likesCount) {
        this.likesCount = likesCount;
    }

    public void setCommentsCount(int commentsCount) {
        this.commentsCount = commentsCount;
    }

    public String getVisibility() {
        return visibility != null ? visibility : "public";
    }

    // Helper methods
    public String getFormattedLikes() {
        if (likesCount == 0) return "Be first to like";
        if (likesCount == 1) return "1 like";
        return likesCount + " likes";
    }

    public String getFormattedComments() {
        if (commentsCount == 0) return "";
        if (commentsCount == 1) return "1 comment";
        return commentsCount + " comments";
    }

    public boolean hasDescription() {
        return description != null && !description.trim().isEmpty();
    }

    public boolean hasTags() {
        return tags != null && tags.length > 0;
    }
}
