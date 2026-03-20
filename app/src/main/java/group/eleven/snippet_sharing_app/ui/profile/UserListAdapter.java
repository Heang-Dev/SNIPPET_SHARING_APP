package group.eleven.snippet_sharing_app.ui.profile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import group.eleven.snippet_sharing_app.R;
import group.eleven.snippet_sharing_app.data.model.User;

/**
 * Adapter for displaying users in followers/following lists
 */
public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.UserViewHolder> {

    private List<User> users = new ArrayList<>();
    private OnUserClickListener listener;
    private String currentUserId;

    public interface OnUserClickListener {
        void onUserClick(User user);
        void onFollowClick(User user, int position);
    }

    public UserListAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setOnUserClickListener(OnUserClickListener listener) {
        this.listener = listener;
    }

    public void setUsers(List<User> users) {
        this.users = users != null ? users : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void updateFollowState(int position, boolean isFollowing) {
        if (position >= 0 && position < users.size()) {
            users.get(position).setFollowing(isFollowing);
            notifyItemChanged(position);
        }
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_follow, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        holder.bind(user, position);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView ivUserAvatar;
        private final TextView tvFullName;
        private final TextView tvUsername;
        private final TextView tvBio;
        private final MaterialButton btnFollow;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            tvFullName = itemView.findViewById(R.id.tvFullName);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvBio = itemView.findViewById(R.id.tvBio);
            btnFollow = itemView.findViewById(R.id.btnFollow);
        }

        private void updateFollowButton(Context context, MaterialButton btn, boolean isFollowing) {
            if (isFollowing) {
                btn.setText("Following");
                btn.setStrokeColorResource(R.color.primary);
                btn.setStrokeWidth(2);
                btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(context, android.R.color.transparent)));
                btn.setTextColor(ContextCompat.getColor(context, R.color.primary));
            } else {
                btn.setText("Follow");
                btn.setStrokeWidth(0);
                btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.primary)));
                btn.setTextColor(ContextCompat.getColor(context, R.color.on_primary));
            }
        }

        void bind(User user, int position) {
            // Set user info
            String displayName = user.getFullName();
            if (displayName == null || displayName.isEmpty()) {
                displayName = user.getUsername();
            }
            tvFullName.setText(displayName);
            tvUsername.setText("@" + user.getUsername());

            // Set bio if available
            if (user.getBio() != null && !user.getBio().isEmpty()) {
                tvBio.setText(user.getBio());
                tvBio.setVisibility(View.VISIBLE);
            } else {
                tvBio.setVisibility(View.GONE);
            }

            // Load avatar
            String avatarUrl = user.getEffectiveAvatarUrl();
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(avatarUrl)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .into(ivUserAvatar);
            } else {
                ivUserAvatar.setImageResource(R.drawable.ic_person);
            }

            // Hide follow button for current user
            if (currentUserId != null && currentUserId.equals(user.getId())) {
                btnFollow.setVisibility(View.GONE);
            } else {
                btnFollow.setVisibility(View.VISIBLE);
                updateFollowButton(itemView.getContext(), btnFollow, user.isFollowing());
            }

            // Click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(user);
                }
            });

            btnFollow.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFollowClick(user, getAdapterPosition());
                }
            });
        }
    }
}
