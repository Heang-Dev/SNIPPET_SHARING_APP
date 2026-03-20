package group.eleven.snippet_sharing_app.ui.snippet;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.widget.NestedScrollView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import group.eleven.snippet_sharing_app.R;
import group.eleven.snippet_sharing_app.data.model.Comment;
import group.eleven.snippet_sharing_app.data.model.Snippet;
import group.eleven.snippet_sharing_app.data.model.User;
import group.eleven.snippet_sharing_app.data.repository.CommentRepository;
import group.eleven.snippet_sharing_app.data.repository.FavoritesRepository;
import group.eleven.snippet_sharing_app.data.repository.FollowRepository;
import group.eleven.snippet_sharing_app.data.repository.SnippetRepository;
import group.eleven.snippet_sharing_app.ui.search.UserProfileActivity;
import group.eleven.snippet_sharing_app.utils.Resource;
import group.eleven.snippet_sharing_app.utils.SessionManager;
import group.eleven.snippet_sharing_app.utils.SyntaxHighlighter;

public class SnippetDetailActivity extends AppCompatActivity {

    public static final String EXTRA_SNIPPET_ID = "extra_snippet_id";

    private SnippetRepository snippetRepository;
    private FavoritesRepository favoritesRepository;
    private FollowRepository followRepository;
    private CommentRepository commentRepository;
    private SessionManager sessionManager;
    private SyntaxHighlighter syntaxHighlighter;

    private Snippet currentSnippet;
    private boolean isSaved = false;
    private boolean isFollowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snippet_detail);

        snippetRepository = new SnippetRepository(this);
        favoritesRepository = new FavoritesRepository(this);
        followRepository = new FollowRepository(this);
        commentRepository = new CommentRepository(this);
        sessionManager = new SessionManager(this);
        syntaxHighlighter = new SyntaxHighlighter(this);

        setupStatusBar();
        setupToolbar();

        String snippetId = getIntent().getStringExtra(EXTRA_SNIPPET_ID);
        if (snippetId == null || snippetId.isEmpty()) {
            Toast.makeText(this, "Snippet not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadSnippet(snippetId);
    }

    private void setupStatusBar() {
        android.view.Window window = getWindow();
        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(R.attr.surfaceColor, typedValue, true);
        int surfaceColor = typedValue.resourceId != 0
                ? ContextCompat.getColor(this, typedValue.resourceId)
                : typedValue.data;
        window.setStatusBarColor(surfaceColor);
        window.setNavigationBarColor(surfaceColor);

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller != null) {
            double darkness = 1 - (0.299 * android.graphics.Color.red(surfaceColor)
                    + 0.587 * android.graphics.Color.green(surfaceColor)
                    + 0.114 * android.graphics.Color.blue(surfaceColor)) / 255;
            controller.setAppearanceLightStatusBars(darkness < 0.5);
            controller.setAppearanceLightNavigationBars(darkness < 0.5);
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setNavigationOnClickListener(v -> finish());

        ImageView ivShare = findViewById(R.id.ivShare);
        ivShare.setOnClickListener(v -> { if (currentSnippet != null) shareSnippet(); });
    }

    private void loadSnippet(String snippetId) {
        snippetRepository.getSnippetBySlug(snippetId).observe(this, resource -> {
            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                currentSnippet = resource.data;
                showSnippet(resource.data);
                loadInlineComments(resource.data.getId());
            } else if (resource.status == Resource.Status.ERROR) {
                hideLoading();
                Toast.makeText(this, "Failed to load snippet", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void showSnippet(Snippet snippet) {
        hideLoading();

        TextView tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        tvToolbarTitle.setText(snippet.getTitle());

        // Author
        CircleImageView ivAuthorAvatar = findViewById(R.id.ivAuthorAvatar);
        TextView tvAuthorName = findViewById(R.id.tvAuthorName);
        TextView tvAuthorUsername = findViewById(R.id.tvAuthorUsername);

        Snippet.SnippetUser user = snippet.getUser();
        if (user != null) {
            String name = user.getFullName();
            if (name == null || name.isEmpty()) name = user.getUsername();
            tvAuthorName.setText(name != null ? name : "Anonymous");
            tvAuthorUsername.setText(user.getUsername() != null ? "@" + user.getUsername() : "");

            String avatarUrl = user.getAvatarUrl();
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                Glide.with(this).load(avatarUrl)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .into(ivAuthorAvatar);
            }

            // Author click → view profile
            if (user.getUsername() != null) {
                ivAuthorAvatar.setOnClickListener(v -> openUserProfile(user.getUsername()));
                tvAuthorName.setOnClickListener(v -> openUserProfile(user.getUsername()));
            }
        }

        // Follow button
        MaterialButton btnFollow = findViewById(R.id.btnFollow);
        String currentUserId = sessionManager.getUser() != null ? sessionManager.getUser().getId() : null;
        if (user != null && currentUserId != null && !currentUserId.equals(user.getId())) {
            btnFollow.setVisibility(View.VISIBLE);
            btnFollow.setOnClickListener(v -> toggleFollow(user.getUsername(), btnFollow));
        } else {
            btnFollow.setVisibility(View.GONE);
        }

        // Title & Description
        TextView tvTitle = findViewById(R.id.tvTitle);
        TextView tvDescription = findViewById(R.id.tvDescription);
        tvTitle.setText(snippet.getTitle());

        String description = snippet.getDescription();
        if (description != null && !description.isEmpty()) {
            tvDescription.setText(description);
            tvDescription.setVisibility(View.VISIBLE);
        }

        // Tags
        List<Snippet.SnippetTag> tags = snippet.getTags();
        LinearLayout tagsContainer = findViewById(R.id.tagsContainer);
        TextView tvTag1 = findViewById(R.id.tvTag1);
        TextView tvTag2 = findViewById(R.id.tvTag2);
        TextView tvTag3 = findViewById(R.id.tvTag3);
        if (tags != null && !tags.isEmpty()) {
            tagsContainer.setVisibility(View.VISIBLE);
            bindTag(tvTag1, tags.size() > 0 ? tags.get(0).getName() : null);
            bindTag(tvTag2, tags.size() > 1 ? tags.get(1).getName() : null);
            bindTag(tvTag3, tags.size() > 2 ? tags.get(2).getName() : null);
        }

        // Stats
        TextView tvLikesCount = findViewById(R.id.tvLikesCount);
        TextView tvCommentsCount = findViewById(R.id.tvCommentsCount);
        TextView tvViewsCount = findViewById(R.id.tvViewsCount);
        int likes = snippet.getFavoriteCount();
        tvLikesCount.setText(likes == 1 ? "1 like" : likes + " likes");
        int comments = snippet.getCommentCount();
        tvCommentsCount.setText(comments == 1 ? "1 comment" : comments + " comments");
        int views = snippet.getViewCount();
        tvViewsCount.setText(views == 1 ? "1 view" : views + " views");

        // Language badge
        TextView tvLanguageBadge = findViewById(R.id.tvLanguageBadge);
        tvLanguageBadge.setText(snippet.getLanguageName());
        try {
            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setCornerRadius(8f);
            int langColor = snippet.getLanguageColor();
            badgeBg.setColor(langColor != 0 ? langColor : ContextCompat.getColor(this, R.color.primary));
            tvLanguageBadge.setBackground(badgeBg);
        } catch (Exception ignored) {}

        // Full code
        TextView tvCode = findViewById(R.id.tvCode);
        String code = snippet.getCode();
        if (code != null && !code.isEmpty()) {
            SpannableString highlighted = syntaxHighlighter.highlightForLanguage(code, snippet.getLanguageName());
            tvCode.setText(highlighted);
        } else {
            tvCode.setText("// No code available");
        }

        // Copy code button
        ImageView ivCopyCode = findViewById(R.id.ivCopyCode);
        ivCopyCode.setOnClickListener(v -> {
            String codeToCopy = snippet.getCode();
            if (codeToCopy != null) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("snippet_code", codeToCopy);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Code copied!", Toast.LENGTH_SHORT).show();
            }
        });

        // Comments section header
        TextView tvCommentsHeader = findViewById(R.id.tvCommentsHeader);
        tvCommentsHeader.setText(comments + " Comments");

        // My avatar in write comment row
        CircleImageView ivCommentAvatar = findViewById(R.id.ivCommentAvatar);
        User me = sessionManager.getUser();
        if (me != null) {
            String myAvatar = me.getEffectiveAvatarUrl();
            if (myAvatar != null && !myAvatar.isEmpty()) {
                Glide.with(this).load(myAvatar)
                        .placeholder(R.drawable.ic_person)
                        .circleCrop()
                        .into(ivCommentAvatar);
            }
        }

        // Write comment + View all → open comments sheet
        View layoutWriteComment = findViewById(R.id.layoutWriteComment);
        TextView tvViewAllComments = findViewById(R.id.tvViewAllComments);
        layoutWriteComment.setOnClickListener(v -> openCommentsSheet(snippet));
        tvViewAllComments.setOnClickListener(v -> openCommentsSheet(snippet));

        // Save state
        isSaved = snippet.isFavorited();
        updateSaveButton();

        // Action buttons
        setupActionButtons(snippet);

        // Show content + action bar
        NestedScrollView scrollContent = findViewById(R.id.scrollContent);
        scrollContent.setVisibility(View.VISIBLE);
        LinearLayout actionBar = findViewById(R.id.actionBar);
        actionBar.setVisibility(View.VISIBLE);
    }

    private void openUserProfile(String username) {
        Intent intent = new Intent(this, UserProfileActivity.class);
        intent.putExtra(UserProfileActivity.EXTRA_USERNAME, username);
        startActivity(intent);
    }

    private void toggleFollow(String username, MaterialButton btnFollow) {
        if (isFollowing) {
            followRepository.unfollowUser(username).observe(this, r -> {
                if (r.status == Resource.Status.SUCCESS) {
                    isFollowing = false;
                    updateFollowButton(btnFollow);
                }
            });
        } else {
            followRepository.followUser(username).observe(this, r -> {
                if (r.status == Resource.Status.SUCCESS) {
                    isFollowing = true;
                    updateFollowButton(btnFollow);
                }
            });
        }
    }

    private void updateFollowButton(MaterialButton btnFollow) {
        if (isFollowing) {
            btnFollow.setText("Following");
            btnFollow.setBackgroundTintList(null);
            btnFollow.setStrokeColorResource(R.color.selective_yellow);
            btnFollow.setStrokeWidth(2);
            btnFollow.setTextColor(ContextCompat.getColor(this, R.color.selective_yellow));
        } else {
            btnFollow.setText("Follow");
            btnFollow.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(this, R.color.selective_yellow)));
            btnFollow.setTextColor(ContextCompat.getColor(this, R.color.on_primary));
            btnFollow.setStrokeWidth(0);
        }
    }

    private void loadInlineComments(String snippetId) {
        commentRepository.getComments(snippetId, 3).observe(this, resource -> {
            if (resource.status == Resource.Status.SUCCESS && resource.data != null && !resource.data.isEmpty()) {
                renderInlineComments(resource.data);
            }
        });
    }

    private void renderInlineComments(List<Comment> comments) {
        LinearLayout container = findViewById(R.id.layoutInlineComments);
        if (container == null) return;
        container.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);
        int limit = Math.min(comments.size(), 3);
        for (int i = 0; i < limit; i++) {
            Comment comment = comments.get(i);
            View itemView = inflater.inflate(R.layout.item_inline_comment, container, false);

            CircleImageView ivAvatar = itemView.findViewById(R.id.ivCommentItemAvatar);
            TextView tvName = itemView.findViewById(R.id.tvCommentAuthor);
            TextView tvBody = itemView.findViewById(R.id.tvCommentBody);

            tvName.setText(comment.getAuthorName());
            tvBody.setText(comment.getContent() != null ? comment.getContent() : "");

            String avatar = comment.getAuthorAvatar();
            if (avatar != null && !avatar.isEmpty()) {
                Glide.with(this).load(avatar)
                        .placeholder(R.drawable.ic_person)
                        .circleCrop()
                        .into(ivAvatar);
            }

            container.addView(itemView);
        }
    }

    private void openCommentsSheet(Snippet snippet) {
        group.eleven.snippet_sharing_app.ui.comment.CommentsBottomSheet sheet =
                group.eleven.snippet_sharing_app.ui.comment.CommentsBottomSheet
                        .newInstance(snippet.getId(), snippet.getTitle());
        sheet.show(getSupportFragmentManager(), "CommentsBottomSheet");
    }

    private void bindTag(TextView tv, String tagName) {
        if (tagName != null && !tagName.isEmpty()) {
            tv.setText("#" + tagName.replace("#", ""));
            tv.setVisibility(View.VISIBLE);
        } else {
            tv.setVisibility(View.GONE);
        }
    }

    private void setupActionButtons(Snippet snippet) {
        // Like
        LinearLayout btnLike = findViewById(R.id.btnLike);
        ImageView ivLike = findViewById(R.id.ivLike);
        TextView tvLike = findViewById(R.id.tvLike);
        btnLike.setOnClickListener(v -> {
            boolean nowLiked = !snippet.isFavorited();
            if (nowLiked) {
                ivLike.setImageResource(R.drawable.ic_heart_filled);
                ivLike.setColorFilter(ContextCompat.getColor(this, R.color.error));
                tvLike.setTextColor(ContextCompat.getColor(this, R.color.error));
                tvLike.setText("Liked");
            } else {
                ivLike.setImageResource(R.drawable.ic_heart);
                ivLike.clearColorFilter();
                tvLike.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
                tvLike.setText("Like");
            }
        });

        // Comment
        LinearLayout btnComment = findViewById(R.id.btnComment);
        btnComment.setOnClickListener(v -> openCommentsSheet(snippet));

        // Save
        LinearLayout btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> toggleSave(snippet));

        // Share
        LinearLayout btnShareAction = findViewById(R.id.btnShareAction);
        btnShareAction.setOnClickListener(v -> shareSnippet());
    }

    private void toggleSave(Snippet snippet) {
        if (isSaved) {
            favoritesRepository.removeFromFavorites(snippet.getId()).observe(this, r -> {
                if (r.status == Resource.Status.SUCCESS) {
                    isSaved = false;
                    updateSaveButton();
                    Toast.makeText(this, "Removed from saved", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            favoritesRepository.addToFavorites(snippet.getId()).observe(this, r -> {
                if (r.status == Resource.Status.SUCCESS) {
                    isSaved = true;
                    updateSaveButton();
                    Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateSaveButton() {
        ImageView ivSave = findViewById(R.id.ivSave);
        TextView tvSave = findViewById(R.id.tvSave);
        if (isSaved) {
            ivSave.setImageResource(R.drawable.ic_star_filled);
            ivSave.setColorFilter(ContextCompat.getColor(this, R.color.selective_yellow));
            tvSave.setText("Saved");
            tvSave.setTextColor(ContextCompat.getColor(this, R.color.selective_yellow));
        } else {
            ivSave.setImageResource(R.drawable.ic_star_outline);
            ivSave.clearColorFilter();
            tvSave.setText("Save");
            tvSave.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        }
    }

    private void shareSnippet() {
        if (currentSnippet == null) return;
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, currentSnippet.getTitle());
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                currentSnippet.getTitle() + "\n\n" + currentSnippet.getCodePreview() + "\n\nShared via Snippet G11");
        startActivity(Intent.createChooser(shareIntent, "Share snippet"));
    }

    private void hideLoading() {
        FrameLayout layoutLoading = findViewById(R.id.layoutLoading);
        if (layoutLoading != null) layoutLoading.setVisibility(View.GONE);
    }
}
