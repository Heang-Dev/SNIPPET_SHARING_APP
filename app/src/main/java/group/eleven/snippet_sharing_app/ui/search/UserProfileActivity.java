package group.eleven.snippet_sharing_app.ui.search;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import group.eleven.snippet_sharing_app.R;
import group.eleven.snippet_sharing_app.api.ApiClient;
import group.eleven.snippet_sharing_app.api.ApiService;
import group.eleven.snippet_sharing_app.data.model.ApiResponse;
import group.eleven.snippet_sharing_app.data.model.SnippetCard;
import group.eleven.snippet_sharing_app.data.repository.FollowRepository;
import group.eleven.snippet_sharing_app.ui.home.SnippetCardAdapter;
import group.eleven.snippet_sharing_app.ui.snippet.SnippetDetailActivity;
import group.eleven.snippet_sharing_app.utils.Resource;
import group.eleven.snippet_sharing_app.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserProfileActivity extends AppCompatActivity {

    public static final String EXTRA_USERNAME = "extra_username";
    public static final String EXTRA_USER_ID = "extra_user_id";
    public static final String EXTRA_FULL_NAME = "extra_full_name";
    public static final String EXTRA_BIO = "extra_bio";
    public static final String EXTRA_AVATAR_URL = "extra_avatar_url";
    public static final String EXTRA_FOLLOWERS = "extra_followers";
    public static final String EXTRA_FOLLOWING = "extra_following";
    public static final String EXTRA_IS_FOLLOWING = "extra_is_following";

    private FollowRepository followRepository;
    private SessionManager sessionManager;

    private String userId;
    private String username;
    private boolean isFollowing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        followRepository = new FollowRepository(this);
        sessionManager = new SessionManager(this);

        username = getIntent().getStringExtra(EXTRA_USERNAME);
        userId = getIntent().getStringExtra(EXTRA_USER_ID);

        if (username == null || username.isEmpty()) {
            finish();
            return;
        }

        setupStatusBar();
        setupToolbar();
        showProfileFromExtras();
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

        TextView tvToolbarUsername = findViewById(R.id.tvToolbarUsername);
        tvToolbarUsername.setText("@" + username);
    }

    private void showProfileFromExtras() {
        Intent intent = getIntent();

        // Avatar
        CircleImageView ivAvatar = findViewById(R.id.ivAvatar);
        String avatarUrl = intent.getStringExtra(EXTRA_AVATAR_URL);
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(this).load(avatarUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(ivAvatar);
        }

        // Name
        TextView tvFullName = findViewById(R.id.tvFullName);
        String fullName = intent.getStringExtra(EXTRA_FULL_NAME);
        tvFullName.setText(fullName != null && !fullName.isEmpty() ? fullName : username);

        // Username
        TextView tvUsername = findViewById(R.id.tvUsername);
        tvUsername.setText("@" + username);

        // Bio
        TextView tvBio = findViewById(R.id.tvBio);
        String bio = intent.getStringExtra(EXTRA_BIO);
        if (bio != null && !bio.isEmpty()) {
            tvBio.setText(bio);
            tvBio.setVisibility(View.VISIBLE);
        }

        // Stats
        int followers = intent.getIntExtra(EXTRA_FOLLOWERS, 0);
        int following = intent.getIntExtra(EXTRA_FOLLOWING, 0);
        ((TextView) findViewById(R.id.tvFollowersCount)).setText(String.valueOf(followers));
        ((TextView) findViewById(R.id.tvFollowingCount)).setText(String.valueOf(following));
        // Snippets count will update after loading snippets

        // Follow / Invite buttons
        MaterialButton btnFollow = findViewById(R.id.btnFollow);
        MaterialButton btnInvite = findViewById(R.id.btnInviteTeam);

        String currentUserId = sessionManager.getUser() != null ? sessionManager.getUser().getId() : null;
        String currentUsername = sessionManager.getUser() != null ? sessionManager.getUser().getUsername() : null;

        if (username.equals(currentUsername)) {
            btnFollow.setVisibility(View.GONE);
            btnInvite.setVisibility(View.GONE);
        } else {
            isFollowing = intent.getBooleanExtra(EXTRA_IS_FOLLOWING, false);
            updateFollowButton(btnFollow);
            btnFollow.setOnClickListener(v -> toggleFollow(btnFollow));
            btnInvite.setOnClickListener(v ->
                    Toast.makeText(this, "Invite to team — coming soon", Toast.LENGTH_SHORT).show());
        }

        // Hide loading, show content
        FrameLayout layoutLoading = findViewById(R.id.layoutLoading);
        if (layoutLoading != null) layoutLoading.setVisibility(View.GONE);
        NestedScrollView scrollContent = findViewById(R.id.scrollContent);
        scrollContent.setVisibility(View.VISIBLE);

        // Load snippets
        if (userId != null) loadUserSnippets(userId);
    }

    private void toggleFollow(MaterialButton btnFollow) {
        if (isFollowing) {
            followRepository.unfollowUser(username).observe(this, r -> {
                if (r.status == Resource.Status.SUCCESS) {
                    isFollowing = false;
                    updateFollowButton(btnFollow);
                } else if (r.status == Resource.Status.ERROR) {
                    Toast.makeText(this, "Failed to unfollow", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            followRepository.followUser(username).observe(this, r -> {
                if (r.status == Resource.Status.SUCCESS) {
                    isFollowing = true;
                    updateFollowButton(btnFollow);
                } else if (r.status == Resource.Status.ERROR) {
                    Toast.makeText(this, "Failed to follow", Toast.LENGTH_SHORT).show();
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
            btnFollow.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.selective_yellow)));
            btnFollow.setTextColor(ContextCompat.getColor(this, R.color.on_primary));
            btnFollow.setStrokeWidth(0);
        }
    }

    private void loadUserSnippets(String userId) {
        // Use the public snippets search filtered by user_id
        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("per_page", "10");
        apiService.getUserSnippets(userId, params).enqueue(new Callback<ApiResponse<java.util.List<group.eleven.snippet_sharing_app.data.model.Snippet>>>() {
            @Override
            public void onResponse(Call<ApiResponse<java.util.List<group.eleven.snippet_sharing_app.data.model.Snippet>>> call,
                                   Response<ApiResponse<java.util.List<group.eleven.snippet_sharing_app.data.model.Snippet>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<group.eleven.snippet_sharing_app.data.model.Snippet> snippets = response.body().getData();
                    List<SnippetCard> cards = new ArrayList<>();
                    for (group.eleven.snippet_sharing_app.data.model.Snippet s : snippets) {
                        cards.add(s.toSnippetCard());
                    }
                    ((TextView) findViewById(R.id.tvSnippetsCount)).setText(String.valueOf(cards.size()));
                    showSnippets(cards);
                } else {
                    showSnippetsEmpty();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<java.util.List<group.eleven.snippet_sharing_app.data.model.Snippet>>> call, Throwable t) {
                showSnippetsEmpty();
            }
        });
    }

    private void showSnippets(List<SnippetCard> cards) {
        if (cards == null || cards.isEmpty()) {
            showSnippetsEmpty();
            return;
        }
        RecyclerView rvSnippets = findViewById(R.id.rvSnippets);
        rvSnippets.setLayoutManager(new LinearLayoutManager(this));
        SnippetCardAdapter adapter = new SnippetCardAdapter(cards);
        adapter.setOnSnippetClickListener(card -> {
            String id = card.getSlug() != null ? card.getSlug() : card.getId();
            if (id != null) {
                Intent intent = new Intent(this, SnippetDetailActivity.class);
                intent.putExtra(SnippetDetailActivity.EXTRA_SNIPPET_ID, id);
                startActivity(intent);
            }
        });
        rvSnippets.setAdapter(adapter);
        rvSnippets.setVisibility(View.VISIBLE);
    }

    private void showSnippetsEmpty() {
        LinearLayout layoutEmpty = findViewById(R.id.layoutSnippetsEmpty);
        if (layoutEmpty != null) layoutEmpty.setVisibility(View.VISIBLE);
    }
}
