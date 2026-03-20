package group.eleven.snippet_sharing_app.data.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Model for snippet data from /snippets API
 */
public class Snippet {

    @SerializedName("id")
    private String id;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("category_id")
    private String categoryId;

    @SerializedName("team_id")
    private String teamId;

    @SerializedName("title")
    private String title;

    @SerializedName("slug")
    private String slug;

    @SerializedName("description")
    private String description;

    @SerializedName("code")
    private String code;

    @SerializedName("privacy")
    private String privacy;

    @SerializedName("view_count")
    private int viewCount;

    @SerializedName("favorite_count")
    private int favoriteCount;

    @SerializedName("comment_count")
    private int commentCount;

    @SerializedName("fork_count")
    private int forkCount;

    @SerializedName("parent_snippet_id")
    private String parentSnippetId;

    @SerializedName("is_fork")
    private boolean isFork;

    @SerializedName("version_number")
    private int versionNumber;

    @SerializedName("is_featured")
    private boolean isFeatured;

    // Language can be either a string (slug) or an object depending on the endpoint
    // We use a nested class and custom deserializer to handle both formats
    @SerializedName("language")
    private SnippetLanguage language;

    // Nested class for language (handles object format)
    public static class SnippetLanguage {
        @SerializedName("id")
        String id;

        @SerializedName("name")
        String name;

        @SerializedName("slug")
        String slug;

        @SerializedName("display_name")
        String displayName;

        @SerializedName("color")
        String color;

        // Constructor for string-only format
        public SnippetLanguage(String slug) {
            this.slug = slug;
            this.name = slug;
        }

        public String getId() { return id; }
        public String getName() { return name != null ? name : slug; }
        public String getSlug() { return slug != null ? slug : name; }
        public String getDisplayName() { return displayName != null ? displayName : getName(); }
        public String getColor() { return color; }
    }

    /**
     * Custom deserializer that handles "language" being either a string or object.
     * Manually parses JSON object fields to avoid infinite recursion
     * (context.deserialize would re-invoke this same deserializer).
     */
    public static class SnippetLanguageDeserializer implements JsonDeserializer<SnippetLanguage> {
        @Override
        public SnippetLanguage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonPrimitive()) {
                // "language": "javascript"
                return new SnippetLanguage(json.getAsString());
            } else if (json.isJsonObject()) {
                // "language": {"id": "...", "name": "JavaScript", "slug": "javascript", ...}
                JsonObject obj = json.getAsJsonObject();
                SnippetLanguage lang = new SnippetLanguage(
                        obj.has("slug") && !obj.get("slug").isJsonNull() ? obj.get("slug").getAsString() : null
                );
                if (obj.has("id") && !obj.get("id").isJsonNull()) lang.id = obj.get("id").getAsString();
                if (obj.has("name") && !obj.get("name").isJsonNull()) lang.name = obj.get("name").getAsString();
                if (obj.has("display_name") && !obj.get("display_name").isJsonNull()) lang.displayName = obj.get("display_name").getAsString();
                if (obj.has("color") && !obj.get("color").isJsonNull()) lang.color = obj.get("color").getAsString();
                return lang;
            }
            return new SnippetLanguage("unknown");
        }
    }

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    @SerializedName("category")
    private SnippetCategory category;

    @SerializedName("tags")
    private List<SnippetTag> tags;

    @SerializedName("user")
    private SnippetUser user;

    @SerializedName("is_favorited")
    private boolean isFavorited;

    @SerializedName("is_owner")
    private boolean isOwner;

    // Nested class for category
    public static class SnippetCategory {
        @SerializedName("id")
        private String id;

        @SerializedName("name")
        private String name;

        @SerializedName("slug")
        private String slug;

        @SerializedName("description")
        private String description;

        @SerializedName("icon")
        private String icon;

        @SerializedName("color")
        private String color;

        public String getId() { return id; }
        public String getName() { return name; }
        public String getSlug() { return slug; }
        public String getDescription() { return description; }
        public String getIcon() { return icon; }
        public String getColor() { return color; }
    }

    // Nested class for tags
    public static class SnippetTag {
        @SerializedName("id")
        private String id;

        @SerializedName("name")
        private String name;

        @SerializedName("slug")
        private String slug;

        @SerializedName("color")
        private String color;

        public String getId() { return id; }
        public String getName() { return name; }
        public String getSlug() { return slug; }
        public String getColor() { return color; }
    }

    // Nested class for user
    public static class SnippetUser {
        @SerializedName("id")
        private String id;

        @SerializedName("username")
        private String username;

        @SerializedName("full_name")
        private String fullName;

        @SerializedName("avatar_url")
        private String avatarUrl;

        public String getId() { return id; }
        public String getUsername() { return username; }
        public String getFullName() { return fullName; }
        public String getAvatarUrl() { return avatarUrl; }
    }

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getCategoryId() { return categoryId; }
    public String getTeamId() { return teamId; }
    public String getTitle() { return title; }
    public String getSlug() { return slug; }
    public String getDescription() { return description; }
    public String getCode() { return code; }
    public String getPrivacy() { return privacy; }
    public int getViewCount() { return viewCount; }
    public int getFavoriteCount() { return favoriteCount; }
    public int getCommentCount() { return commentCount; }
    public int getForkCount() { return forkCount; }
    public String getParentSnippetId() { return parentSnippetId; }
    public boolean isFork() { return isFork; }
    public int getVersionNumber() { return versionNumber; }
    public boolean isFeatured() { return isFeatured; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public SnippetLanguage getLanguage() { return language; }
    public String getLanguageString() {
        return language != null ? language.getSlug() : null;
    }
    public SnippetCategory getCategory() { return category; }
    public List<SnippetTag> getTags() { return tags; }
    public SnippetUser getUser() { return user; }
    public boolean isFavorited() { return isFavorited; }
    public boolean isOwner() { return isOwner; }

    /**
     * Get language name or default
     */
    public String getLanguageName() {
        if (language != null) {
            String name = language.getDisplayName();
            return name != null && !name.isEmpty() ? name : "Unknown";
        }
        return "Unknown";
    }

    /**
     * Get language color for badge based on language name
     */
    public int getLanguageColor() {
        // Use color from API if available
        if (language != null && language.getColor() != null) {
            try {
                return android.graphics.Color.parseColor(language.getColor());
            } catch (Exception ignored) {}
        }

        String slug = language != null ? language.getSlug() : null;
        if (slug == null) return android.graphics.Color.parseColor("#6B7280");

        // Fallback: common language colors
        switch (slug.toLowerCase()) {
            case "javascript": case "js": return android.graphics.Color.parseColor("#F7DF1E");
            case "python": case "py": return android.graphics.Color.parseColor("#3776AB");
            case "java": return android.graphics.Color.parseColor("#ED8B00");
            case "typescript": case "ts": return android.graphics.Color.parseColor("#3178C6");
            case "php": return android.graphics.Color.parseColor("#777BB4");
            case "ruby": case "rb": return android.graphics.Color.parseColor("#CC342D");
            case "go": case "golang": return android.graphics.Color.parseColor("#00ADD8");
            case "rust": case "rs": return android.graphics.Color.parseColor("#DEA584");
            case "swift": return android.graphics.Color.parseColor("#FA7343");
            case "kotlin": case "kt": return android.graphics.Color.parseColor("#7F52FF");
            case "c": return android.graphics.Color.parseColor("#A8B9CC");
            case "cpp": case "c++": return android.graphics.Color.parseColor("#00599C");
            case "csharp": case "c#": return android.graphics.Color.parseColor("#239120");
            case "html": return android.graphics.Color.parseColor("#E34F26");
            case "css": return android.graphics.Color.parseColor("#1572B6");
            case "sql": return android.graphics.Color.parseColor("#4479A1");
            case "shell": case "bash": return android.graphics.Color.parseColor("#4EAA25");
            default: return android.graphics.Color.parseColor("#6B7280");
        }
    }

    /**
     * Get language badge text (first 2-3 chars)
     */
    public String getLanguageBadge() {
        String name = language != null ? language.getSlug() : null;
        if (name != null && !name.isEmpty()) {
            if (name.length() <= 3) return name.toUpperCase();
            return name.substring(0, 2).toUpperCase();
        }
        return "??";
    }

    /**
     * Get code preview (first few lines)
     */
    public String getCodePreview() {
        if (code == null || code.isEmpty()) {
            return "// No code preview";
        }
        String[] lines = code.split("\n");
        StringBuilder preview = new StringBuilder();
        for (int i = 0; i < Math.min(8, lines.length); i++) {
            preview.append(lines[i]);
            if (i < Math.min(8, lines.length) - 1) {
                preview.append("\n");
            }
        }
        return preview.toString();
    }

    /**
     * Get tag names as array
     */
    public String[] getTagNames() {
        if (tags == null || tags.isEmpty()) {
            return new String[] { privacy != null ? privacy : "public" };
        }
        String[] names = new String[Math.min(tags.size(), 3)];
        for (int i = 0; i < names.length; i++) {
            names[i] = tags.get(i).getName();
        }
        return names;
    }

    /**
     * Get formatted updated time
     */
    public String getFormattedUpdateTime() {
        if (updatedAt == null) return "";

        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", java.util.Locale.US);
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.util.Date date = sdf.parse(updatedAt);

            if (date != null) {
                long diff = System.currentTimeMillis() - date.getTime();
                long minutes = diff / (1000 * 60);
                long hours = diff / (1000 * 60 * 60);
                long days = diff / (1000 * 60 * 60 * 24);

                if (minutes < 1) return "Updated just now";
                if (minutes < 60) return "Updated " + minutes + " min ago";
                if (hours < 24) return "Updated " + hours + "h ago";
                if (days < 7) return "Updated " + days + "d ago";
                return "Updated " + days / 7 + "w ago";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Updated recently";
    }

    /**
     * Convert to SnippetCard for adapter compatibility
     */
    public SnippetCard toSnippetCard() {
        String authorName = user != null ? user.getFullName() : null;
        String authorAvatar = user != null ? user.getAvatarUrl() : null;
        String authorUsername = user != null ? user.getUsername() : null;

        if (authorName == null || authorName.isEmpty()) {
            authorName = authorUsername;
        }

        SnippetCard card = new SnippetCard(
                id,
                title,
                description,
                getLanguageName(),
                getFormattedUpdateTime(),
                getCodePreview(),
                getTagNames(),
                getLanguageColor(),
                authorName,
                authorAvatar,
                authorUsername,
                favoriteCount,
                commentCount,
                isFavorited,
                privacy != null ? privacy : "public"
        );
        card.setSlug(slug);
        return card;
    }
}
