package group.eleven.snippet_sharing_app.ui.search;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import group.eleven.snippet_sharing_app.R;
import group.eleven.snippet_sharing_app.data.model.User;
import group.eleven.snippet_sharing_app.data.repository.FollowRepository;
import group.eleven.snippet_sharing_app.data.repository.SearchRepository;
import group.eleven.snippet_sharing_app.ui.profile.ProfileActivity;
import group.eleven.snippet_sharing_app.utils.KeyboardUtils;
import group.eleven.snippet_sharing_app.utils.Resource;
import group.eleven.snippet_sharing_app.utils.SessionManager;

public class SearchActivity extends AppCompatActivity {

    private static final String TAG = "SearchActivity";
    private static final long DEBOUNCE_MS = 400;

    private RecyclerView rvSearchResults;
    private UserSearchAdapter adapter;
    private EditText etSearch;
    private CircleImageView ivProfile;
    private TextView tvResultCount;
    private TextView tvSectionTitle;
    private FrameLayout layoutLoading;
    private LinearLayout layoutEmpty;

    private SessionManager sessionManager;
    private SearchRepository searchRepository;
    private FollowRepository followRepository;
    private Handler searchHandler;
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        sessionManager = new SessionManager(this);
        searchRepository = new SearchRepository(this);
        followRepository = new FollowRepository(this);
        searchHandler = new Handler(Looper.getMainLooper());

        initViews();
        loadProfileImage();
        setupListeners();
        loadSuggestedUsers();
    }

    private void initViews() {
        rvSearchResults = findViewById(R.id.rvSearchResults);
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));

        String currentUserId = sessionManager.getUser() != null ? sessionManager.getUser().getId() : null;
        adapter = new UserSearchAdapter(currentUserId);
        rvSearchResults.setAdapter(adapter);

        etSearch = findViewById(R.id.etSearch);
        ivProfile = findViewById(R.id.ivProfile);
        tvResultCount = findViewById(R.id.tvResultCount);
        tvSectionTitle = findViewById(R.id.tvSectionTitle);
        layoutLoading = findViewById(R.id.layoutLoading);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        adapter.setOnUserClickListener(user -> {
            String uname = user.getUsername();
            if (uname != null) {
                Intent intent = new Intent(this, UserProfileActivity.class);
                intent.putExtra(UserProfileActivity.EXTRA_USERNAME, uname);
                startActivity(intent);
            }
        });

        adapter.setOnFollowClickListener((user, position) -> {
            String uname = user.getUsername();
            if (uname == null) return;
            if (user.isFollowing()) {
                followRepository.unfollowUser(uname).observe(this, r -> {
                    if (r.status == Resource.Status.SUCCESS) {
                        adapter.updateFollowState(position, false);
                    }
                });
            } else {
                followRepository.followUser(uname).observe(this, r -> {
                    if (r.status == Resource.Status.SUCCESS) {
                        adapter.updateFollowState(position, true);
                    }
                });
            }
        });
    }

    private void loadProfileImage() {
        User user = sessionManager.getUser();
        if (user != null) {
            String avatarUrl = user.getEffectiveAvatarUrl();
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                Glide.with(this).load(avatarUrl)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .circleCrop()
                        .into(ivProfile);
            }
        }
    }

    private void loadSuggestedUsers() {
        tvSectionTitle.setText("Suggested People");
        showLoading(true);

        searchRepository.searchUsers("", 20).observe(this, resource -> {
            showLoading(false);
            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                adapter.setUsers(resource.data);
                updateCount(resource.data.size());
                showEmpty(resource.data.isEmpty());
            } else if (resource.status == Resource.Status.ERROR) {
                adapter.setUsers(new ArrayList<>());
                showEmpty(true);
            }
        });
    }

    private void performSearch(String query) {
        if (query.trim().isEmpty()) {
            loadSuggestedUsers();
            return;
        }

        tvSectionTitle.setText("Results for \"" + query + "\"");
        showLoading(true);

        searchRepository.searchUsers(query.trim(), 30).observe(this, resource -> {
            showLoading(false);
            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                adapter.setUsers(resource.data);
                updateCount(resource.data.size());
                showEmpty(resource.data.isEmpty());
            } else if (resource.status == Resource.Status.ERROR) {
                Log.e(TAG, "Search failed: " + resource.message);
                showEmpty(true);
            }
        });
    }

    private void setupListeners() {
        // Back
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Profile image
        ivProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));

        // Clear
        findViewById(R.id.btnClear).setOnClickListener(v -> {
            etSearch.setText("");
            loadSuggestedUsers();
        });

        // Search text with debounce
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> performSearch(s.toString());
                searchHandler.postDelayed(searchRunnable, DEBOUNCE_MS);
            }
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                performSearch(etSearch.getText().toString());
                return true;
            }
            return false;
        });

        // Dismiss keyboard on outside touch
        View rootView = findViewById(android.R.id.content);
        KeyboardUtils.setupKeyboardDismissOnOutsideTouch(this, rootView);
    }

    private void showLoading(boolean show) {
        layoutLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            rvSearchResults.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void showEmpty(boolean show) {
        layoutEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
        rvSearchResults.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void updateCount(int count) {
        if (tvResultCount != null) {
            tvResultCount.setText(count > 0 ? count + " found" : "");
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        KeyboardUtils.handleTouchOutsideEditText(this, event);
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
    }
}
