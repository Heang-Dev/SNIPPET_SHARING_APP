package group.eleven.snippet_sharing_app.ui.home;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

import group.eleven.snippet_sharing_app.R;
import group.eleven.snippet_sharing_app.data.model.SnippetCard;
import group.eleven.snippet_sharing_app.data.model.User;
import group.eleven.snippet_sharing_app.data.repository.DashboardRepository;
import group.eleven.snippet_sharing_app.databinding.ActivityHomeBinding;
import group.eleven.snippet_sharing_app.ui.auth.LoginActivity;
import group.eleven.snippet_sharing_app.ui.team.TeamsListActivity;
import group.eleven.snippet_sharing_app.ui.snippet.CreateSnippetActivity;
import group.eleven.snippet_sharing_app.ui.profile.ProfileActivity;
import group.eleven.snippet_sharing_app.ui.profile.AccountSettingsActivity;
import group.eleven.snippet_sharing_app.ui.notification.NotificationsActivity;
import group.eleven.snippet_sharing_app.ui.search.SearchActivity;
import group.eleven.snippet_sharing_app.data.repository.AuthRepository;
import group.eleven.snippet_sharing_app.utils.BottomNavHelper;
import group.eleven.snippet_sharing_app.data.repository.FavoritesRepository;
import group.eleven.snippet_sharing_app.utils.KeyboardUtils;
import group.eleven.snippet_sharing_app.utils.Resource;
import group.eleven.snippet_sharing_app.utils.SessionManager;

/**
 * Home Activity - Social feed with Facebook-style layout
 */
public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "HomeActivity";

    private ActivityHomeBinding binding;
    private SessionManager sessionManager;
    private DashboardRepository dashboardRepository;
    private AuthRepository authRepository;
    private ActionBarDrawerToggle drawerToggle;

    private FeedSnippetAdapter feedAdapter;
    private FavoritesRepository favoritesRepository;
    private List<SnippetCard> snippetList = new ArrayList<>();

    // Drawer header views
    private ImageView ivDrawerUserProfile;
    private TextView tvDrawerSnippetsCount, tvDrawerFollowersCount, tvDrawerFollowingCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Starting HomeActivity");

        try {
            // Inflate the layout
            binding = ActivityHomeBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            // Setup theme-aware status bar
            setupStatusBar();

            // Initialize session manager and repositories
            sessionManager = new SessionManager(this);
            dashboardRepository = new DashboardRepository(this);
            authRepository = new AuthRepository(this);
            favoritesRepository = new FavoritesRepository(this);

            if (!sessionManager.isLoggedIn()) {
                navigateToLogin();
                return;
            }

            // Setup toolbar
            setSupportActionBar(binding.toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(false);
            }

            // Setup keyboard dismiss on outside touch
            KeyboardUtils.setupKeyboardDismissOnOutsideTouch(this, binding.getRoot());

            // Setup navigation drawer
            setupNavigationDrawer();

            // Setup UI components
            setupUserInfo();
            setupFeed();
            setupSwipeRefresh();
            setupClickListeners();

            // Load initial data
            loadFeed();

            Log.d(TAG, "onCreate: Setup completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "onCreate: Error", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart: Refreshing feed");
        loadFeed();
    }

    /**
     * Setup status bar and navigation bar colors based on current theme (light/dark)
     */
    private void setupStatusBar() {
        android.view.Window window = getWindow();

        // Get the surface color from theme to match app bar and bottom nav
        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(R.attr.surfaceColor, typedValue, true);
        int surfaceColor;
        if (typedValue.resourceId != 0) {
            surfaceColor = androidx.core.content.ContextCompat.getColor(this, typedValue.resourceId);
        } else {
            surfaceColor = typedValue.data;
        }

        // Set status bar and navigation bar colors to match surface
        window.setStatusBarColor(surfaceColor);
        window.setNavigationBarColor(surfaceColor);

        // Set icon colors based on theme
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller != null) {
            // Check if we're in light mode (background is light colored)
            boolean isLightBackground = isColorLight(surfaceColor);
            // Light background = dark icons (true), Dark background = light icons (false)
            controller.setAppearanceLightStatusBars(isLightBackground);
            controller.setAppearanceLightNavigationBars(isLightBackground);
        }
    }

    /**
     * Determine if a color is light or dark
     */
    private boolean isColorLight(int color) {
        double darkness = 1 - (0.299 * android.graphics.Color.red(color)
                + 0.587 * android.graphics.Color.green(color)
                + 0.114 * android.graphics.Color.blue(color)) / 255;
        return darkness < 0.5;
    }

    private void setupNavigationDrawer() {
        // Setup drawer toggle
        drawerToggle = new ActionBarDrawerToggle(
                this, binding.drawerLayout, binding.toolbar,
                R.string.drawer_open, R.string.drawer_close);
        binding.drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        // Setup navigation view
        binding.navigationView.setNavigationItemSelectedListener(this);

        // Setup drawer header
        View headerView = binding.navigationView.getHeaderView(0);
        TextView tvUserName = headerView.findViewById(R.id.tvUserName);
        TextView tvUserEmail = headerView.findViewById(R.id.tvUserEmail);
        ivDrawerUserProfile = headerView.findViewById(R.id.ivUserProfile);
        tvDrawerSnippetsCount = headerView.findViewById(R.id.tvSnippetsCount);
        tvDrawerFollowersCount = headerView.findViewById(R.id.tvFollowersCount);
        tvDrawerFollowingCount = headerView.findViewById(R.id.tvFollowingCount);

        User user = sessionManager.getUser();
        if (user != null) {
            // Set display name
            String displayName = user.getFullName() != null && !user.getFullName().isEmpty()
                    ? user.getFullName()
                    : user.getUsername();
            tvUserName.setText(displayName);

            // Set email
            if (tvUserEmail != null && user.getEmail() != null) {
                tvUserEmail.setText(user.getEmail());
            }

            // Load avatar image
            String drawerAvatarUrl = user.getEffectiveAvatarUrl();
            if (ivDrawerUserProfile != null && drawerAvatarUrl != null && !drawerAvatarUrl.isEmpty()) {
                Glide.with(this)
                        .load(drawerAvatarUrl)
                        .placeholder(R.drawable.ic_user_avatar)
                        .error(R.drawable.ic_user_avatar)
                        .circleCrop()
                        .into(ivDrawerUserProfile);
            }
        }

        // Set stats from User object (loaded from session)
        updateDrawerStats(user);

        // Refresh user data from API to get latest stats
        refreshUserProfile();
    }

    /**
     * Update drawer header stats with user data
     */
    private void updateDrawerStats(User user) {
        if (user != null) {
            if (tvDrawerSnippetsCount != null) {
                tvDrawerSnippetsCount.setText(String.valueOf(user.getSnippetsCount()));
            }
            if (tvDrawerFollowersCount != null) {
                tvDrawerFollowersCount.setText(String.valueOf(user.getFollowersCount()));
            }
            if (tvDrawerFollowingCount != null) {
                tvDrawerFollowingCount.setText(String.valueOf(user.getFollowingCount()));
            }
        } else {
            if (tvDrawerSnippetsCount != null) tvDrawerSnippetsCount.setText("0");
            if (tvDrawerFollowersCount != null) tvDrawerFollowersCount.setText("0");
            if (tvDrawerFollowingCount != null) tvDrawerFollowingCount.setText("0");
        }
    }

    /**
     * Refresh user profile from API to get latest stats
     */
    private void refreshUserProfile() {
        authRepository.getCurrentUser().observe(this, resource -> {
            if (resource.isSuccess() && resource.getData() != null) {
                User freshUser = resource.getData().getUser();
                if (freshUser != null) {
                    // Update session with fresh user data
                    sessionManager.updateUser(freshUser);
                    // Update drawer stats
                    updateDrawerStats(freshUser);
                    Log.d(TAG, "User profile refreshed: " + freshUser.toString());
                }
            } else if (resource.isError()) {
                Log.w(TAG, "Failed to refresh user profile: " + resource.getMessage());
                // Continue with cached data, don't show error to user
            }
        });
    }

    private void setupUserInfo() {
        // Load user avatar in create post card
        User user = sessionManager.getUser();
        if (user != null) {
            String avatarUrl = user.getEffectiveAvatarUrl();
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                Glide.with(this)
                        .load(avatarUrl)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .into(binding.ivUserAvatar);
            }
        }
    }

    private void setupFeed() {
        // Setup RecyclerView with feed adapter
        binding.rvRecentSnippets.setLayoutManager(new LinearLayoutManager(this));
        feedAdapter = new FeedSnippetAdapter(new ArrayList<>());
        feedAdapter.setOnFeedItemClickListener(new FeedSnippetAdapter.OnFeedItemClickListener() {
            @Override
            public void onSnippetClick(SnippetCard snippet) {
                String id = snippet.getSlug() != null ? snippet.getSlug() : snippet.getId();
                if (id != null) {
                    Intent intent = new Intent(HomeActivity.this,
                            group.eleven.snippet_sharing_app.ui.snippet.SnippetDetailActivity.class);
                    intent.putExtra(group.eleven.snippet_sharing_app.ui.snippet.SnippetDetailActivity.EXTRA_SNIPPET_ID, id);
                    startActivity(intent);
                }
            }

            @Override
            public void onLikeClick(SnippetCard snippet, int position) {
                if (snippet.getId() == null) {
                    Toast.makeText(HomeActivity.this, "Cannot like this snippet", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Optimistically update UI
                boolean newLikeState = !snippet.isLiked();
                int newCount = snippet.getLikesCount() + (newLikeState ? 1 : -1);
                feedAdapter.updateLikeState(position, newLikeState, newCount);

                // Call API to persist like/unlike
                dashboardRepository.toggleLike(snippet.getId(), snippet.isLiked()).observe(HomeActivity.this, resource -> {
                    if (resource.status == Resource.Status.ERROR) {
                        // Revert on error
                        feedAdapter.updateLikeState(position, snippet.isLiked(), snippet.getLikesCount());
                        Toast.makeText(HomeActivity.this, "Failed to update like", Toast.LENGTH_SHORT).show();
                    }
                });
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
                String authorUsername = snippet.getAuthorUsername();
                if (authorUsername != null && !authorUsername.isEmpty()) {
                    Intent intent = new Intent(HomeActivity.this,
                            group.eleven.snippet_sharing_app.ui.search.UserProfileActivity.class);
                    intent.putExtra(group.eleven.snippet_sharing_app.ui.search.UserProfileActivity.EXTRA_USERNAME, authorUsername);
                    intent.putExtra(group.eleven.snippet_sharing_app.ui.search.UserProfileActivity.EXTRA_FULL_NAME, snippet.getAuthorName());
                    intent.putExtra(group.eleven.snippet_sharing_app.ui.search.UserProfileActivity.EXTRA_AVATAR_URL, snippet.getAuthorAvatar());
                    startActivity(intent);
                }
            }

            @Override
            public void onMoreOptionsClick(SnippetCard snippet, View anchor) {
                showSnippetOptions(snippet, anchor);
            }

            @Override
            public void onSaveClick(SnippetCard snippet, int position) {
                boolean wasSaved = snippet.isLiked();
                if (wasSaved) {
                    favoritesRepository.removeFromFavorites(snippet.getId()).observe(HomeActivity.this, r -> {
                        if (r.status == Resource.Status.SUCCESS) {
                            feedAdapter.updateSaveState(position, false);
                            Toast.makeText(HomeActivity.this, "Removed from favorites", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    favoritesRepository.addToFavorites(snippet.getId()).observe(HomeActivity.this, r -> {
                        if (r.status == Resource.Status.SUCCESS) {
                            feedAdapter.updateSaveState(position, true);
                            Toast.makeText(HomeActivity.this, "Saved to favorites", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
        binding.rvRecentSnippets.setAdapter(feedAdapter);
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.primary);
        binding.swipeRefresh.setOnRefreshListener(this::loadFeed);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh bottom nav selection
        if (binding.bottomNav != null) {
            binding.bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    /**
     * Load public snippets for social feed
     */
    private void loadFeed() {
        dashboardRepository.getPublicSnippets(20).observe(this, resource -> {
            binding.swipeRefresh.setRefreshing(false);

            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                snippetList.clear();
                snippetList.addAll(resource.data);
                feedAdapter.setSnippets(snippetList);
                updateEmptyState(snippetList.isEmpty());
            } else if (resource.status == Resource.Status.ERROR) {
                Log.e(TAG, "Failed to load feed: " + resource.message);
                loadTrendingFallback();
            } else if (resource.status == Resource.Status.LOADING) {
                // Show loading state
                if (snippetList.isEmpty()) {
                    binding.swipeRefresh.setRefreshing(true);
                }
            }
        });
    }

    private void loadTrendingFallback() {
        dashboardRepository.getTrendingSnippets(15).observe(this, resource -> {
            binding.swipeRefresh.setRefreshing(false);

            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                snippetList.clear();
                snippetList.addAll(resource.data);
                feedAdapter.setSnippets(snippetList);
                updateEmptyState(snippetList.isEmpty());
            } else if (resource.status == Resource.Status.ERROR) {
                Log.e(TAG, "Failed to load trending: " + resource.message);
                // Show empty state when no data is available
                updateEmptyState(true);
                Toast.makeText(this, "Unable to load snippets. Please check your connection.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateEmptyState(boolean isEmpty) {
        if (binding.emptyState != null) {
            binding.emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
        binding.rvRecentSnippets.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void shareSnippet(SnippetCard snippet) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, snippet.getTitle());
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                snippet.getTitle() + "\n\n" + snippet.getCodePreview() + "\n\nShared via Snippet G11");
        startActivity(Intent.createChooser(shareIntent, "Share snippet"));
    }

    private void showSnippetOptions(SnippetCard snippet, View anchor) {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_snippet_options, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_copy_code) {
                copyToClipboard(snippet.getCodePreview());
                return true;
            } else if (id == R.id.action_save) {
                Toast.makeText(this, "Saved to favorites", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.action_report) {
                Toast.makeText(this, "Report - Coming Soon", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void copyToClipboard(String text) {
        android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("code", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Code copied!", Toast.LENGTH_SHORT).show();
    }


    private void setupClickListeners() {
        // Search icon
        binding.ivSearch.setOnClickListener(v -> {
            Intent intent = new Intent(this, SearchActivity.class);
            startActivity(intent);
        });

        // Notification icon
        binding.ivNotification.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationsActivity.class);
            startActivity(intent);
        });

        // Create post card
        binding.cardCreatePost.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateSnippetActivity.class);
            startActivity(intent);
        });

        // Filter feed
        binding.tvFilterFeed.setOnClickListener(v -> {
            Toast.makeText(this, "Filter - Coming Soon", Toast.LENGTH_SHORT).show();
        });

        // Empty state create button
        if (binding.btnCreateFirst != null) {
            binding.btnCreateFirst.setOnClickListener(v -> {
                Intent intent = new Intent(this, CreateSnippetActivity.class);
                startActivity(intent);
            });
        }

        // FAB
        binding.fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateSnippetActivity.class);
            startActivity(intent);
        });

        // Bottom Navigation
        BottomNavHelper.applyNavigationBarInsets(binding.bottomNavContainer);
        BottomNavHelper.applyFabNavigationBarInsets(binding.fab);
        binding.bottomNav.setSelectedItemId(R.id.nav_home);
        BottomNavHelper.setupProfileAvatar(this, binding.bottomNav, sessionManager);

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_teams) {
                Intent intent = new Intent(this, TeamsListActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_favorites) {
                Intent intent = new Intent(this,
                        group.eleven.snippet_sharing_app.ui.favorites.FavoritesActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            // Already on Home - just close drawer
        } else if (id == R.id.nav_explore) {
            Intent intent = new Intent(this, group.eleven.snippet_sharing_app.ui.explore.ExploreActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_all_snippets) {
            Intent intent = new Intent(this, group.eleven.snippet_sharing_app.ui.mysnippets.MySnippetsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_create_snippet) {
            Intent intent = new Intent(this, CreateSnippetActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_favorites) {
            Intent intent = new Intent(this, group.eleven.snippet_sharing_app.ui.favorites.FavoritesActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_shared) {
            Intent intent = new Intent(this, group.eleven.snippet_sharing_app.ui.shared.SharedWithMeActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_teams) {
            Intent intent = new Intent(HomeActivity.this, TeamsListActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_notifications) {
            Intent intent = new Intent(this, NotificationsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_profile) {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_preferences) {
            Intent intent = new Intent(this, AccountSettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_help) {
            Intent intent = new Intent(this, group.eleven.snippet_sharing_app.ui.help.HelpSupportActivity.class);
            startActivity(intent);
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @SuppressLint({ "MissingSuperCall", "GestureBackNavigation" })
    @Override
    public void onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            // Show logout confirmation
            showLogoutConfirmation();
        }
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.profile_logout)
                .setMessage(R.string.profile_logout_confirm)
                .setPositiveButton(R.string.profile_logout, (dialog, which) -> logout())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void logout() {
        sessionManager.logout();
        Toast.makeText(this, getString(R.string.profile_logout_success), Toast.LENGTH_SHORT).show();
        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showCommentsBottomSheet(SnippetCard snippet) {
        group.eleven.snippet_sharing_app.ui.comment.CommentsBottomSheet bottomSheet =
                group.eleven.snippet_sharing_app.ui.comment.CommentsBottomSheet.newInstance(
                        snippet.getId() != null ? snippet.getId() : "snippet_1",
                        snippet.getTitle()
                );
        bottomSheet.setOnCommentCountChangeListener((snippetId, newCount) -> {
            // Update the snippet card's comment count
            feedAdapter.updateCommentCount(snippetId, newCount);
        });
        bottomSheet.show(getSupportFragmentManager(), "CommentsBottomSheet");
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        KeyboardUtils.handleTouchOutsideEditText(this, event);
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}