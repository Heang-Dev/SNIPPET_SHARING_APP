package group.eleven.snippet_sharing_app.ui.favorites;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import group.eleven.snippet_sharing_app.R;
import group.eleven.snippet_sharing_app.data.model.SnippetCard;
import group.eleven.snippet_sharing_app.data.repository.DashboardRepository;
import group.eleven.snippet_sharing_app.data.repository.FavoritesRepository;
import group.eleven.snippet_sharing_app.databinding.ActivityFavoritesBinding;
import group.eleven.snippet_sharing_app.ui.explore.ExploreActivity;
import group.eleven.snippet_sharing_app.ui.home.HomeActivity;
import group.eleven.snippet_sharing_app.ui.home.SnippetCardAdapter;
import group.eleven.snippet_sharing_app.ui.profile.ProfileActivity;
import group.eleven.snippet_sharing_app.ui.snippet.CreateSnippetActivity;
import group.eleven.snippet_sharing_app.ui.team.TeamsListActivity;
import group.eleven.snippet_sharing_app.utils.BottomNavHelper;
import group.eleven.snippet_sharing_app.utils.Resource;
import group.eleven.snippet_sharing_app.utils.SessionManager;

/**
 * FavoritesActivity - Display user's starred/favorite snippets
 */
public class FavoritesActivity extends AppCompatActivity {

    private static final String TAG = "FavoritesActivity";

    private ActivityFavoritesBinding binding;
    private SessionManager sessionManager;
    private DashboardRepository dashboardRepository;
    private FavoritesRepository favoritesRepository;
    private SnippetCardAdapter adapter;
    private List<SnippetCard> allFavorites = new ArrayList<>();
    private List<SnippetCard> filteredFavorites = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityFavoritesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupStatusBar();

        sessionManager = new SessionManager(this);
        dashboardRepository = new DashboardRepository(this);
        favoritesRepository = new FavoritesRepository(this);

        setupToolbar();
        setupSearch();
        setupRecyclerView();
        setupBottomNavigation();
        setupClickListeners();

        loadFavorites();
    }

    private void setupStatusBar() {
        android.view.Window window = getWindow();
        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(R.attr.surfaceColor, typedValue, true);
        int surfaceColor;
        if (typedValue.resourceId != 0) {
            surfaceColor = androidx.core.content.ContextCompat.getColor(this, typedValue.resourceId);
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

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterFavorites(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupRecyclerView() {
        binding.rvFavorites.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SnippetCardAdapter(new ArrayList<>());
        adapter.setOnSnippetClickListener(snippet -> {
            String id = snippet.getSlug() != null ? snippet.getSlug() : snippet.getId();
            if (id != null) {
                Intent intent = new Intent(this,
                        group.eleven.snippet_sharing_app.ui.snippet.SnippetDetailActivity.class);
                intent.putExtra(group.eleven.snippet_sharing_app.ui.snippet.SnippetDetailActivity.EXTRA_SNIPPET_ID, id);
                startActivity(intent);
            }
        });
        // Tapping the star in Favorites removes the snippet from favorites
        adapter.setOnFavoriteClickListener((snippet, position) -> {
            favoritesRepository.removeFromFavorites(snippet.getId()).observe(this, r -> {
                if (r.status == Resource.Status.SUCCESS) {
                    allFavorites.removeIf(s -> s.getId() != null && s.getId().equals(snippet.getId()));
                    updateUI();
                    Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show();
                } else if (r.status == Resource.Status.ERROR) {
                    Toast.makeText(this, "Error: " + r.message, Toast.LENGTH_SHORT).show();
                }
            });
        });
        binding.rvFavorites.setAdapter(adapter);
    }

    private void setupBottomNavigation() {
        // Apply navigation bar insets
        BottomNavHelper.applyNavigationBarInsets(binding.bottomNavContainer);


        // Set Favorites as selected
        binding.bottomNav.setSelectedItemId(R.id.nav_favorites);

        // Setup profile avatar
        BottomNavHelper.setupProfileAvatar(this, binding.bottomNav, sessionManager);

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Intent intent = new Intent(this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_teams) {
                Intent intent = new Intent(this, TeamsListActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_favorites) {
                // Already on Favorites
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

    private void setupClickListeners() {
        binding.btnExplore.setOnClickListener(v -> {
            Intent intent = new Intent(this, ExploreActivity.class);
            startActivity(intent);
        });

        binding.swipeRefresh.setOnRefreshListener(this::loadFavorites);
    }

    private void loadFavorites() {
        binding.swipeRefresh.setRefreshing(true);

        favoritesRepository.getFavoriteSnippets(30).observe(this, resource -> {
            binding.swipeRefresh.setRefreshing(false);

            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                allFavorites.clear();
                allFavorites.addAll(resource.data);
                updateUI();
                Log.d(TAG, "Loaded " + allFavorites.size() + " favorites from API");
            } else if (resource.status == Resource.Status.ERROR) {
                Log.e(TAG, "Failed to load favorites: " + resource.message);
                // Show empty state when no data available
                allFavorites.clear();
                updateUI();
                Toast.makeText(this, "Unable to load favorites", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterFavorites(String query) {
        filteredFavorites.clear();

        if (query.isEmpty()) {
            filteredFavorites.addAll(allFavorites);
        } else {
            String lowerQuery = query.toLowerCase();
            for (SnippetCard snippet : allFavorites) {
                if (snippet.getTitle().toLowerCase().contains(lowerQuery) ||
                    snippet.getLanguageBadge().toLowerCase().contains(lowerQuery) ||
                    (snippet.getDescription() != null && snippet.getDescription().toLowerCase().contains(lowerQuery))) {
                    filteredFavorites.add(snippet);
                }
            }
        }

        adapter.filterList(filteredFavorites);
        updateCount(filteredFavorites.size());
    }

    private void updateUI() {
        if (allFavorites.isEmpty()) {
            binding.rvFavorites.setVisibility(View.GONE);
            binding.layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            binding.rvFavorites.setVisibility(View.VISIBLE);
            binding.layoutEmpty.setVisibility(View.GONE);
            filteredFavorites.clear();
            filteredFavorites.addAll(allFavorites);
            adapter.filterList(filteredFavorites);
        }
        updateCount(allFavorites.size());
    }

    private void updateCount(int count) {
        String text = count == 1 ? "1 favorite" : count + " favorites";
        binding.tvFavoritesCount.setText(text);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure correct nav item is selected when returning
        binding.bottomNav.setSelectedItemId(R.id.nav_favorites);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
