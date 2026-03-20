package group.eleven.snippet_sharing_app.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import group.eleven.snippet_sharing_app.R;
import group.eleven.snippet_sharing_app.api.ApiClient;
import group.eleven.snippet_sharing_app.api.ApiService;
import group.eleven.snippet_sharing_app.data.model.ApiResponse;
import group.eleven.snippet_sharing_app.data.model.User;
import group.eleven.snippet_sharing_app.data.repository.FollowRepository;
import group.eleven.snippet_sharing_app.utils.Resource;
import group.eleven.snippet_sharing_app.utils.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Bottom sheet for displaying followers or following users
 */
public class UsersBottomSheet extends BottomSheetDialogFragment {

    public static final String TYPE_FOLLOWERS = "followers";
    public static final String TYPE_FOLLOWING = "following";

    private static final String ARG_USERNAME = "username";
    private static final String ARG_TYPE = "type";

    private String username;
    private String type;

    private RecyclerView rvUsers;
    private LinearLayout layoutEmpty;
    private FrameLayout layoutLoading;
    private TextView tvTitle;
    private TextView tvEmptyTitle;
    private TextView tvEmptyMessage;
    private ImageView ivClose;

    private UserListAdapter adapter;
    private ApiService apiService;
    private SessionManager sessionManager;
    private FollowRepository followRepository;

    public static UsersBottomSheet newInstance(String username, String type) {
        UsersBottomSheet fragment = new UsersBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_USERNAME, username);
        args.putString(ARG_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            username = getArguments().getString(ARG_USERNAME);
            type = getArguments().getString(ARG_TYPE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_users, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize
        apiService = ApiClient.getApiService(requireContext());
        sessionManager = new SessionManager(requireContext());
        followRepository = new FollowRepository(requireContext());

        // Find views
        rvUsers = view.findViewById(R.id.rvUsers);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        layoutLoading = view.findViewById(R.id.layoutLoading);
        tvTitle = view.findViewById(R.id.tvTitle);
        tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
        ivClose = view.findViewById(R.id.ivClose);

        // Setup header
        setupHeader();

        // Setup RecyclerView
        setupRecyclerView();

        // Load data
        loadUsers();

        // Close button
        ivClose.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onStart() {
        super.onStart();
        // Expand the bottom sheet
        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        if (dialog != null) {
            FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        }
    }

    private void setupHeader() {
        if (TYPE_FOLLOWERS.equals(type)) {
            tvTitle.setText("Followers");
            tvEmptyTitle.setText("No followers yet");
            tvEmptyMessage.setText("Share your snippets to get followers!");
        } else {
            tvTitle.setText("Following");
            tvEmptyTitle.setText("Not following anyone");
            tvEmptyMessage.setText("Find developers to follow!");
        }
    }

    private void setupRecyclerView() {
        String currentUserId = null;
        User currentUser = sessionManager.getUser();
        if (currentUser != null) {
            currentUserId = currentUser.getId();
        }

        adapter = new UserListAdapter(currentUserId);
        adapter.setOnUserClickListener(new UserListAdapter.OnUserClickListener() {
            @Override
            public void onUserClick(User user) {
                // TODO: Navigate to user profile
                Toast.makeText(requireContext(), "View profile: @" + user.getUsername(), Toast.LENGTH_SHORT).show();
                dismiss();
            }

            @Override
            public void onFollowClick(User user, int position) {
                String uname = user.getUsername();
                if (uname == null) return;
                if (user.isFollowing()) {
                    followRepository.unfollowUser(uname).observe(getViewLifecycleOwner(), r -> {
                        if (r.status == Resource.Status.SUCCESS) {
                            adapter.updateFollowState(position, false);
                            Toast.makeText(requireContext(), "Unfollowed @" + uname, Toast.LENGTH_SHORT).show();
                        } else if (r.status == Resource.Status.ERROR) {
                            Toast.makeText(requireContext(), "Failed to unfollow", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    followRepository.followUser(uname).observe(getViewLifecycleOwner(), r -> {
                        if (r.status == Resource.Status.SUCCESS) {
                            adapter.updateFollowState(position, true);
                            Toast.makeText(requireContext(), "Following @" + uname, Toast.LENGTH_SHORT).show();
                        } else if (r.status == Resource.Status.ERROR) {
                            Toast.makeText(requireContext(), "Failed to follow", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });

        rvUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvUsers.setAdapter(adapter);
    }

    private void loadUsers() {
        showLoading(true);

        Map<String, String> params = new HashMap<>();
        params.put("per_page", "50");

        Call<ApiResponse<List<User>>> call;
        if (TYPE_FOLLOWERS.equals(type)) {
            call = apiService.getUserFollowers(username, params);
        } else {
            call = apiService.getUserFollowing(username, params);
        }

        call.enqueue(new Callback<ApiResponse<List<User>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<User>>> call, Response<ApiResponse<List<User>>> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<User> users = response.body().getData();
                    if (users != null && !users.isEmpty()) {
                        adapter.setUsers(users);
                        showContent();
                    } else {
                        showEmpty();
                    }
                } else {
                    showEmpty();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<User>>> call, Throwable t) {
                showLoading(false);
                showEmpty();
                Toast.makeText(requireContext(), "Failed to load users", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean show) {
        layoutLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            rvUsers.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void showContent() {
        rvUsers.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
    }

    private void showEmpty() {
        rvUsers.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
    }
}
