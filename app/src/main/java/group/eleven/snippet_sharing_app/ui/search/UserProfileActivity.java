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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import group.eleven.snippet_sharing_app.R;
import group.eleven.snippet_sharing_app.api.ApiClient;
import group.eleven.snippet_sharing_app.api.ApiService;
import group.eleven.snippet_sharing_app.data.model.ApiResponse;
import group.eleven.snippet_sharing_app.data.model.Snippet;
import group.eleven.snippet_sharing_app.data.model.SnippetCard;
import group.eleven.snippet_sharing_app.data.model.User;
import group.eleven.snippet_sharing_app.data.repository.FollowRepository;
import group.eleven.snippet_sharing_app.ui.home.SnippetCardAdapter;
import group.eleven.snippet_sharing_app.ui.snippet.SnippetDetailActivity;
import group.eleven.snippet_sharing_app.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserProfileActivity extends AppCompatActivity {

    public static final String EXTRA_USERNAME = "extra_username";

    private FollowRepository followRepository;
    private SessionManager sessionManager;
    private ApiService apiService;

    private User profileUser;
    private boolean isFollowing = false;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        followRepository = new FollowRepository(this);
        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient(this).create(ApiService.class);

        username = getIntent().getStringExtra(EXTRA_USERNAME);
        if (username == null || username.isEmpty()) {
            finish();
            return;
        }

        setupStatusBar();
        setupToolbar();
        loadUserProfile(username);
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

    private void loadUserProfile(String username) {
        apiService.getUserProfile(username).enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    profileUser = response.body().getData();
                    showProfile(profileUser);
                    loadUserSnippets(username);
                } else {
                    hideLoading();
                    Toast.makeText(UserProfileActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                hideLoading();
                Toast.makeText(UserProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void showProfile(User user) {
        hideLoading();

        // Avatar
        CircleImageView ivAvatar = findViewById(R.id.ivAvatar);
        String avatarUrl = user.getEffectiveAvatarUrl();
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(this).load(avatarUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(ivAvatar);
        }

        // Name & username
        TextView tvFullName = findViewById(R.id.tvFullName);
        TextView tvUsername = findViewById(R.id.tvUsername);
        String name = user.getFullName();
        if (name == null || name.isEmpty()) name = user.getUsername();
        tvFullName.setText(name != null ? name : "Unknown");
        tvUsername.setText(user.getUsername() != null ? "@" + user.getUsername() : "");

        // Bio
        TextView tvBio = findViewById(R.id.tvBio);
        String bio = user.getBio();
        if (bio != null && !bio.isEmpty()) {
            tvBio.setText(bio);
            tvBio.setVisibility(View.VISIBLE);
        }

        // Stats
        TextView tvSnippetsCount = findViewById(R.id.tvSnippetsCount);
        TextView tvFollowersCount = findViewById(R.id.tvFollowersCount);
        TextView tvFollowingCount = findViewById(R.id.tvFollowingCount);
        tvSnippetsCount.setText(String.valueOf(user.getSnippetsCount()));
        tvFollowersCount.setText(String.valueOf(user.getFollowersCount()));
        tvFollowingCount.setText(String.valueOf(user.getFollowingCount()));

        // Follow / Invite buttons
        MaterialButton btnFollow = findViewById(R.id.btnFollow);
        MaterialButton btnInvite = findViewById(R.id.btnInviteTeam);

        String currentUserId = sessionManager.getUser() != null ? sessionManager.getUser().getId() : null;
        if (currentUserId != null && currentUserId.equals(user.getId())) {
            btnFollow.setVisibility(View.GONE);
            btnInvite.setVisibility(View.GONE);
        } else {
            isFollowing = user.isFollowing();
            updateFollowButton(btnFollow);
            btnFollow.setOnClickListener(v -> toggleFollow(btnFollow));
            btnInvite.setOnClickListener(v ->
                    Toast.makeText(this, "Invite to team — coming soon", Toast.LENGTH_SHORT).show());
        }

        // Toolbar title
        TextView tvToolbarUsername = findViewById(R.id.tvToolbarUsername);
        tvToolbarUsername.setText(user.getUsername() != null ? "@" + user.getUsername() : "Profile");

        // Show content
        NestedScrollView scrollContent = findViewById(R.id.scrollContent);
        scrollContent.setVisibility(View.VISIBLE);
    }

    private void toggleFollow(MaterialButton btnFollow) {
        if (profileUser == null) return;
        if (isFollowing) {
            followRepository.unfollowUser(profileUser.getUsername()).observe(this, r -> {
                if (r.status == group.eleven.snippet_sharing_app.utils.Resource.Status.SUCCESS) {
                    isFollowing = false;
                    updateFollowButton(btnFollow);
                } else if (r.status == group.eleven.snippet_sharing_app.utils.Resource.Status.ERROR) {
                    Toast.makeText(this, "Failed to unfollow", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            followRepository.followUser(profileUser.getUsername()).observe(this, r -> {
                if (r.status == group.eleven.snippet_sharing_app.utils.Resource.Status.SUCCESS) {
                    isFollowing = true;
                    updateFollowButton(btnFollow);
                } else if (r.status == group.eleven.snippet_sharing_app.utils.Resource.Status.ERROR) {
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

    private void loadUserSnippets(String username) {
        Map<String, String> params = new HashMap<>();
        params.put("per_page", "10");
        apiService.getUserSnippets(username, params).enqueue(new Callback<ApiResponse<List<Snippet>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Snippet>>> call, Response<ApiResponse<List<Snippet>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<Snippet> snippets = response.body().getData();
                    showSnippets(snippets);
                } else {
                    showSnippetsEmpty();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Snippet>>> call, Throwable t) {
                showSnippetsEmpty();
            }
        });
    }

    private void showSnippets(List<Snippet> snippets) {
        if (snippets == null || snippets.isEmpty()) {
            showSnippetsEmpty();
            return;
        }

        RecyclerView rvSnippets = findViewById(R.id.rvSnippets);
        rvSnippets.setLayoutManager(new LinearLayoutManager(this));

        List<SnippetCard> cards = new ArrayList<>();
        for (Snippet s : snippets) {
            cards.add(s.toSnippetCard());
        }

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
        layoutEmpty.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        FrameLayout layoutLoading = findViewById(R.id.layoutLoading);
        if (layoutLoading != null) layoutLoading.setVisibility(View.GONE);
    }
}
