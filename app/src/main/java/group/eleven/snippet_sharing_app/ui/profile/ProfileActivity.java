package group.eleven.snippet_sharing_app.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import de.hdodenhof.circleimageview.CircleImageView;
import group.eleven.snippet_sharing_app.R;
import group.eleven.snippet_sharing_app.data.model.Collection;
import group.eleven.snippet_sharing_app.data.model.SnippetCard;
import group.eleven.snippet_sharing_app.data.model.User;
import group.eleven.snippet_sharing_app.data.repository.CollectionsRepository;
import group.eleven.snippet_sharing_app.data.repository.DashboardRepository;
import group.eleven.snippet_sharing_app.data.repository.FavoritesRepository;
import group.eleven.snippet_sharing_app.ui.home.FeedSnippetAdapter;
import group.eleven.snippet_sharing_app.ui.notification.NotificationsActivity;
import group.eleven.snippet_sharing_app.ui.snippet.CollectionDetailActivity;
import group.eleven.snippet_sharing_app.utils.Resource;
import group.eleven.snippet_sharing_app.utils.SessionManager;

/**
 * Profile Activity - Displays user profile with stats and snippets
 */
public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    // Views
    private Toolbar toolbar;
    private ImageView ivSettings;
    private CircleImageView ivProfile;
    private FrameLayout flProfileImage;
    private View vStatusIndicator;
    private TextView tvName, tvUsername, tvBio;
    private LinearLayout llBadges;
    private TextView tvBadgeVerified;
    private TextView tvJoinedDate, tvLocation, tvWebsite;
    private LinearLayout llSocialLinks;
    private TextView tvSnippetsCount, tvFollowersCount, tvFollowingCount, tvLikesCount;
    private TextView tabSnippets, tabStars, tabCollections;
    private View tabIndicatorSnippets, tabIndicatorStars, tabIndicatorCollections;
    private RecyclerView rvContent;
    private LinearLayout layoutEmpty;
    private TextView tvEmptyTitle, tvEmptyMessage;
    private FrameLayout layoutLoading;

    // Data
    private SessionManager sessionManager;
    private DashboardRepository dashboardRepository;
    private FavoritesRepository favoritesRepository;
    private CollectionsRepository collectionsRepository;
    private FeedSnippetAdapter adapter;
    private CollectionAdapter collectionAdapter;
    private String currentTab = "snippets";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sessionManager = new SessionManager(this);
        dashboardRepository = new DashboardRepository(this);
        favoritesRepository = new FavoritesRepository(this);
        collectionsRepository = new CollectionsRepository(this);

        setupStatusBar();
        initViews();
        setupToolbar();
        setupClickListeners();
        setupRecyclerView();
        setupTabs();
    }

    private void setupStatusBar() {
        Window window = getWindow();
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(R.attr.surfaceColor, typedValue, true);
        int surfaceColor;
        if (typedValue.resourceId != 0) {
            surfaceColor = ContextCompat.getColor(this, typedValue.resourceId);
        } else {
            surfaceColor = typedValue.data;
        }
        window.setStatusBarColor(surfaceColor);
        window.setNavigationBarColor(surfaceColor);

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller != null) {
            boolean isLightBackground = isColorLight(surfaceColor);
            controller.setAppearanceLightStatusBars(isLightBackground);
            controller.setAppearanceLightNavigationBars(isLightBackground);
        }
    }

    private boolean isColorLight(int color) {
        double darkness = 1 - (0.299 * android.graphics.Color.red(color)
                + 0.587 * android.graphics.Color.green(color)
                + 0.114 * android.graphics.Color.blue(color)) / 255;
        return darkness < 0.5;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfileData();
        loadContent(currentTab);
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        ivSettings = findViewById(R.id.ivSettings);
        ivProfile = findViewById(R.id.ivProfile);
        flProfileImage = findViewById(R.id.flProfileImage);
        vStatusIndicator = findViewById(R.id.vStatusIndicator);
        tvName = findViewById(R.id.tvName);
        tvUsername = findViewById(R.id.tvUsername);
        tvBio = findViewById(R.id.tvBio);
        llBadges = findViewById(R.id.llBadges);
        tvBadgeVerified = findViewById(R.id.tvBadgeVerified);
        tvJoinedDate = findViewById(R.id.tvJoinedDate);
        tvLocation = findViewById(R.id.tvLocation);
        tvWebsite = findViewById(R.id.tvWebsite);
        llSocialLinks = findViewById(R.id.llSocialLinks);
        tvSnippetsCount = findViewById(R.id.tvSnippetsCount);
        tvFollowersCount = findViewById(R.id.tvFollowersCount);
        tvFollowingCount = findViewById(R.id.tvFollowingCount);
        tvLikesCount = findViewById(R.id.tvLikesCount);
        tabSnippets = findViewById(R.id.tabSnippets);
        tabStars = findViewById(R.id.tabStars);
        tabCollections = findViewById(R.id.tabCollections);
        tabIndicatorSnippets = findViewById(R.id.tabIndicatorSnippets);
        tabIndicatorStars = findViewById(R.id.tabIndicatorStars);
        tabIndicatorCollections = findViewById(R.id.tabIndicatorCollections);
        rvContent = findViewById(R.id.rvContent);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
        layoutLoading = findViewById(R.id.layoutLoading);
    }

    private void setupToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Set navigation icon tint programmatically to ensure theme-awareness
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(R.attr.textPrimaryColor, typedValue, true);
        int textPrimaryColor;
        if (typedValue.resourceId != 0) {
            textPrimaryColor = ContextCompat.getColor(this, typedValue.resourceId);
        } else {
            textPrimaryColor = typedValue.data;
        }

        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().setTint(textPrimaryColor);
        }

        toolbar.setNavigationOnClickListener(v -> navigateBackToHome());
    }

    private void navigateBackToHome() {
        Intent intent = new Intent(this, group.eleven.snippet_sharing_app.ui.home.HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void setupClickListeners() {
        // Settings
        ivSettings.setOnClickListener(v ->
                startActivity(new Intent(this, AccountSettingsActivity.class)));

        // Edit Profile
        findViewById(R.id.btnEditProfile).setOnClickListener(v ->
                startActivity(new Intent(this, EditProfileActivity.class)));

        // Profile image tap -> Edit Profile
        flProfileImage.setOnClickListener(v ->
                startActivity(new Intent(this, EditProfileActivity.class)));

        // Notifications
        findViewById(R.id.cvNotifications).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));

        // Social links (if present)
        View cvGithub = findViewById(R.id.cvGithub);
        View cvTwitter = findViewById(R.id.cvTwitter);
        View cvWebsiteLink = findViewById(R.id.cvWebsiteLink);

        if (cvGithub != null) {
            cvGithub.setOnClickListener(v -> openSocialLink("github"));
        }
        if (cvTwitter != null) {
            cvTwitter.setOnClickListener(v -> openSocialLink("twitter"));
        }
        if (cvWebsiteLink != null) {
            cvWebsiteLink.setOnClickListener(v -> openSocialLink("website"));
        }

        // Stats click listeners - show bottom sheets
        setupStatClickListeners();
    }

    private void setupStatClickListeners() {
        User user = sessionManager.getUser();
        String username = user != null ? user.getUsername() : null;

        if (username == null) return;

        // Followers click
        View llStatFollowers = findViewById(R.id.llStatFollowers);
        if (llStatFollowers != null) {
            llStatFollowers.setOnClickListener(v -> {
                UsersBottomSheet bottomSheet = UsersBottomSheet.newInstance(username, UsersBottomSheet.TYPE_FOLLOWERS);
                bottomSheet.show(getSupportFragmentManager(), "FollowersBottomSheet");
            });
        }

        // Following click
        View llStatFollowing = findViewById(R.id.llStatFollowing);
        if (llStatFollowing != null) {
            llStatFollowing.setOnClickListener(v -> {
                UsersBottomSheet bottomSheet = UsersBottomSheet.newInstance(username, UsersBottomSheet.TYPE_FOLLOWING);
                bottomSheet.show(getSupportFragmentManager(), "FollowingBottomSheet");
            });
        }

        // Snippets click - switch to snippets tab
        View llStatSnippets = findViewById(R.id.llStatSnippets);
        if (llStatSnippets != null) {
            llStatSnippets.setOnClickListener(v -> {
                currentTab = "snippets";
                updateTabUI(tabSnippets);
                loadContent("snippets");
            });
        }

        // Likes click - switch to stars/favorites tab
        View llStatLikes = findViewById(R.id.llStatLikes);
        if (llStatLikes != null) {
            llStatLikes.setOnClickListener(v -> {
                currentTab = "stars";
                updateTabUI(tabStars);
                loadContent("stars");
            });
        }
    }

    private void setupRecyclerView() {
        adapter = new FeedSnippetAdapter(new ArrayList<>());
        adapter.setOnFeedItemClickListener(new FeedSnippetAdapter.OnFeedItemClickListener() {
            @Override
            public void onSnippetClick(SnippetCard snippet) {
                String id = snippet.getSlug() != null ? snippet.getSlug() : snippet.getId();
                if (id != null) {
                    Intent intent = new Intent(ProfileActivity.this,
                            group.eleven.snippet_sharing_app.ui.snippet.SnippetDetailActivity.class);
                    intent.putExtra(group.eleven.snippet_sharing_app.ui.snippet.SnippetDetailActivity.EXTRA_SNIPPET_ID, id);
                    startActivity(intent);
                }
            }

            @Override
            public void onLikeClick(SnippetCard snippet, int position) {
                boolean newLikeState = !snippet.isLiked();
                int newCount = snippet.getLikesCount() + (newLikeState ? 1 : -1);
                adapter.updateLikeState(position, newLikeState, newCount);
            }

            @Override
            public void onCommentClick(SnippetCard snippet) {
                showCommentsBottomSheet(snippet);
            }

            @Override
            public void onShareClick(SnippetCard snippet) {
                shareSnippet(snippet);
            }

            @Override
            public void onAuthorClick(SnippetCard snippet) {
                // Already on profile, do nothing
            }

            @Override
            public void onMoreOptionsClick(SnippetCard snippet, View anchor) {
                // TODO: Show options menu
            }
        });
        collectionAdapter = new CollectionAdapter();
        collectionAdapter.setOnCollectionClickListener(collection -> {
            Intent intent = new Intent(ProfileActivity.this, CollectionDetailActivity.class);
            intent.putExtra(CollectionDetailActivity.EXTRA_COLLECTION_ID, collection.getId());
            intent.putExtra(CollectionDetailActivity.EXTRA_COLLECTION_NAME, collection.getName());
            startActivity(intent);
        });

        rvContent.setLayoutManager(new LinearLayoutManager(this));
        rvContent.setAdapter(adapter);
        rvContent.setNestedScrollingEnabled(false);
    }

    private void setupTabs() {
        tabSnippets.setOnClickListener(v -> {
            currentTab = "snippets";
            updateTabUI(tabSnippets);
            loadContent("snippets");
        });

        tabStars.setOnClickListener(v -> {
            currentTab = "stars";
            updateTabUI(tabStars);
            loadContent("stars");
        });

        tabCollections.setOnClickListener(v -> {
            currentTab = "collections";
            updateTabUI(tabCollections);
            loadContent("collections");
        });
    }

    private void loadProfileData() {
        User user = sessionManager.getUser();

        if (user != null) {
            // Name
            String displayName = user.getFullName();
            if (displayName == null || displayName.isEmpty()) {
                displayName = user.getUsername();
            }
            tvName.setText(displayName != null ? displayName : "User");

            // Username
            String username = user.getUsername();
            tvUsername.setText(username != null ? "@" + username : "");

            // Bio
            String bio = user.getBio();
            if (bio != null && !bio.isEmpty()) {
                tvBio.setText(bio);
                tvBio.setVisibility(View.VISIBLE);
            } else {
                tvBio.setVisibility(View.GONE);
            }

            // Avatar — use effective URL which falls back to OAuth provider avatar
            String avatarUrl = user.getEffectiveAvatarUrl();
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                Glide.with(this)
                        .load(avatarUrl)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .into(ivProfile);
            }

            // Joined date
            String createdAt = user.getCreatedAt();
            if (createdAt != null && !createdAt.isEmpty()) {
                tvJoinedDate.setText("Joined " + formatJoinDate(createdAt));
            } else {
                tvJoinedDate.setText("Joined recently");
            }

            // Website
            String websiteUrl = user.getWebsiteUrl();
            if (websiteUrl != null && !websiteUrl.isEmpty()) {
                tvWebsite.setText(formatWebsiteDisplay(websiteUrl));
                tvWebsite.setVisibility(View.VISIBLE);
            } else {
                tvWebsite.setVisibility(View.GONE);
            }

            // Location
            String location = user.getLocation();
            if (location != null && !location.isEmpty()) {
                tvLocation.setText(location);
                tvLocation.setVisibility(View.VISIBLE);
            } else {
                tvLocation.setVisibility(View.GONE);
            }

            // Social links
            String githubUrl = user.getGithubUrl();
            String twitterUrl = user.getTwitterUrl();
            if ((githubUrl != null && !githubUrl.isEmpty()) ||
                    (twitterUrl != null && !twitterUrl.isEmpty()) ||
                    (websiteUrl != null && !websiteUrl.isEmpty())) {
                llSocialLinks.setVisibility(View.VISIBLE);
            } else {
                llSocialLinks.setVisibility(View.GONE);
            }

            // Badges (show verified if email verified)
            if (user.isEmailVerified()) {
                llBadges.setVisibility(View.VISIBLE);
                tvBadgeVerified.setText("✓");
            } else {
                llBadges.setVisibility(View.GONE);
            }
        } else {
            // Fallback to mock data
            tvName.setText("Guest User");
            tvUsername.setText("@guest");
            tvBio.setVisibility(View.GONE);
            tvJoinedDate.setText("Joined recently");
            tvLocation.setVisibility(View.GONE);
            tvWebsite.setVisibility(View.GONE);
            llSocialLinks.setVisibility(View.GONE);
            llBadges.setVisibility(View.GONE);
        }

        // Load stats from User object
        loadUserStats(user);
    }

    private void loadUserStats(User user) {
        if (user != null) {
            // Use real stats from User object
            tvSnippetsCount.setText(String.valueOf(user.getSnippetsCount()));
            tvFollowersCount.setText(formatCount(user.getFollowersCount()));
            tvFollowingCount.setText(String.valueOf(user.getFollowingCount()));
            tvLikesCount.setText(String.valueOf(user.getLikesReceivedCount()));
        } else {
            tvSnippetsCount.setText("0");
            tvFollowersCount.setText("0");
            tvFollowingCount.setText("0");
            tvLikesCount.setText("0");
        }
    }

    private void loadContent(String type) {
        showLoading(true);

        switch (type) {
            case "stars":
                tvEmptyTitle.setText("No starred snippets");
                tvEmptyMessage.setText("Star snippets to save them for later!");
                rvContent.setAdapter(adapter);
                loadFavorites();
                break;
            case "collections":
                tvEmptyTitle.setText("No collections yet");
                tvEmptyMessage.setText("Create collections to organize your snippets!");
                loadCollections();
                break;
            default: // snippets
                tvEmptyTitle.setText("No snippets yet");
                tvEmptyMessage.setText("Create your first snippet to share!");
                rvContent.setAdapter(adapter);
                loadMySnippets();
                break;
        }
    }

    private void loadMySnippets() {
        dashboardRepository.getRecentSnippets(20).observe(this, resource -> {
            showLoading(false);

            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                adapter.setSnippets(resource.data);
                updateContentVisibility(resource.data.isEmpty());
            } else if (resource.status == Resource.Status.ERROR) {
                // Show empty state when API fails
                adapter.setSnippets(new ArrayList<>());
                updateContentVisibility(true);
            }
        });
    }

    private void loadFavorites() {
        favoritesRepository.getFavoriteSnippets(30).observe(this, resource -> {
            showLoading(false);
            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                adapter.setSnippets(resource.data);
                updateContentVisibility(resource.data.isEmpty());
            } else if (resource.status == Resource.Status.ERROR) {
                adapter.setSnippets(new ArrayList<>());
                updateContentVisibility(true);
            }
        });
    }

    private void loadCollections() {
        rvContent.setAdapter(collectionAdapter);
        collectionsRepository.getMyCollections().observe(this, resource -> {
            showLoading(false);
            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                collectionAdapter.setCollections(resource.data);
                updateContentVisibility(resource.data.isEmpty());
            } else if (resource.status == Resource.Status.ERROR) {
                collectionAdapter.setCollections(new ArrayList<>());
                updateContentVisibility(true);
            }
        });
    }

    private void updateContentVisibility(boolean isEmpty) {
        if (isEmpty) {
            rvContent.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            rvContent.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void showLoading(boolean show) {
        if (layoutLoading != null) {
            layoutLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void updateTabUI(TextView selectedTab) {
        resetTabs();

        // Highlight selected tab
        selectedTab.setTextColor(ContextCompat.getColor(this, R.color.primary));
        selectedTab.setTypeface(null, android.graphics.Typeface.BOLD);

        // Show indicator for selected tab
        if (selectedTab == tabSnippets) {
            tabIndicatorSnippets.setBackgroundColor(ContextCompat.getColor(this, R.color.primary));
        } else if (selectedTab == tabStars) {
            tabIndicatorStars.setBackgroundColor(ContextCompat.getColor(this, R.color.primary));
        } else if (selectedTab == tabCollections) {
            tabIndicatorCollections.setBackgroundColor(ContextCompat.getColor(this, R.color.primary));
        }
    }

    private void resetTabs() {
        TextView[] tabs = {tabSnippets, tabStars, tabCollections};
        View[] indicators = {tabIndicatorSnippets, tabIndicatorStars, tabIndicatorCollections};

        // Get the theme color for muted text
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(R.attr.textMutedColor, typedValue, true);
        int mutedColor = typedValue.data;

        for (TextView tab : tabs) {
            if (tab != null) {
                tab.setTextColor(mutedColor);
                tab.setTypeface(null, android.graphics.Typeface.NORMAL);
            }
        }

        // Hide all indicators
        for (View indicator : indicators) {
            if (indicator != null) {
                indicator.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }
        }
    }

    private void shareSnippet(SnippetCard snippet) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, snippet.getTitle());
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                snippet.getTitle() + "\n\n" + snippet.getCodePreview() + "\n\nShared via Snippet G11");
        startActivity(Intent.createChooser(shareIntent, "Share snippet"));
    }

    private void showCommentsBottomSheet(SnippetCard snippet) {
        group.eleven.snippet_sharing_app.ui.comment.CommentsBottomSheet bottomSheet =
                group.eleven.snippet_sharing_app.ui.comment.CommentsBottomSheet.newInstance(
                        snippet.getId() != null ? snippet.getId() : "snippet_1",
                        snippet.getTitle()
                );
        bottomSheet.setOnCommentCountChangeListener((snippetId, newCount) -> {
            // Update the snippet card's comment count
            adapter.updateCommentCount(snippetId, newCount);
        });
        bottomSheet.show(getSupportFragmentManager(), "CommentsBottomSheet");
    }

    private void openSocialLink(String type) {
        User user = sessionManager.getUser();
        if (user == null) return;

        String url = null;
        switch (type) {
            case "github":
                url = user.getGithubUrl();
                break;
            case "twitter":
                url = user.getTwitterUrl();
                break;
            case "website":
                url = user.getWebsiteUrl();
                break;
        }

        if (url != null && !url.isEmpty()) {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url));
            startActivity(intent);
        }
    }

    private String formatJoinDate(String dateString) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = inputFormat.parse(dateString);
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
            return outputFormat.format(date);
        } catch (Exception e) {
            return "recently";
        }
    }

    private String formatWebsiteDisplay(String url) {
        if (url == null) return "";
        return url.replace("https://", "")
                .replace("http://", "")
                .replace("www.", "");
    }

    private String formatCount(int count) {
        if (count >= 1000000) {
            return String.format(Locale.getDefault(), "%.1fM", count / 1000000.0);
        } else if (count >= 1000) {
            return String.format(Locale.getDefault(), "%.1fk", count / 1000.0);
        }
        return String.valueOf(count);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        // Navigate back to Home instead of just finishing
        navigateBackToHome();
    }
}
