package group.eleven.snippet_sharing_app.ui.search;

import android.content.res.ColorStateList;
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

public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.ViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public interface OnFollowClickListener {
        void onFollowClick(User user, int position);
    }

    private List<User> users = new ArrayList<>();
    private OnUserClickListener userClickListener;
    private OnFollowClickListener followClickListener;
    private String currentUserId;

    public UserSearchAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setOnUserClickListener(OnUserClickListener l) { this.userClickListener = l; }
    public void setOnFollowClickListener(OnFollowClickListener l) { this.followClickListener = l; }

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
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        holder.bind(user, position);
    }

    @Override
    public int getItemCount() { return users.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivAvatar;
        TextView tvFullName, tvUsername, tvBio, tvFollowers;
        MaterialButton btnFollow;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivUserAvatar);
            tvFullName = itemView.findViewById(R.id.tvUserFullName);
            tvUsername = itemView.findViewById(R.id.tvUserUsername);
            tvBio = itemView.findViewById(R.id.tvUserBio);
            tvFollowers = itemView.findViewById(R.id.tvUserFollowers);
            btnFollow = itemView.findViewById(R.id.btnFollowUser);
        }

        void bind(User user, int position) {
            // Name
            String name = user.getFullName();
            if (name == null || name.isEmpty()) name = user.getUsername();
            tvFullName.setText(name != null ? name : "Unknown");

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

            // Followers
            int followers = user.getFollowersCount();
            tvFollowers.setText(followers == 1 ? "1 follower" : followers + " followers");

            // Avatar
            String avatarUrl = user.getEffectiveAvatarUrl();
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                Glide.with(itemView.getContext()).load(avatarUrl)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .circleCrop()
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.drawable.ic_person);
            }

            // Hide follow button for self
            String userId = user.getId();
            if (userId != null && userId.equals(currentUserId)) {
                btnFollow.setVisibility(View.GONE);
            } else {
                btnFollow.setVisibility(View.VISIBLE);
                updateFollowButton(user.isFollowing());
                btnFollow.setOnClickListener(v -> {
                    if (followClickListener != null) {
                        followClickListener.onFollowClick(user, getAdapterPosition());
                    }
                });
            }

            itemView.setOnClickListener(v -> {
                if (userClickListener != null) userClickListener.onUserClick(user);
            });
        }

        void updateFollowButton(boolean isFollowing) {
            if (isFollowing) {
                btnFollow.setText("Following");
                btnFollow.setBackgroundTintList(null);
                btnFollow.setStrokeColorResource(R.color.selective_yellow);
                btnFollow.setStrokeWidth(2);
                btnFollow.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.selective_yellow));
            } else {
                btnFollow.setText("Follow");
                btnFollow.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(itemView.getContext(), R.color.selective_yellow)));
                btnFollow.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.on_primary));
                btnFollow.setStrokeWidth(0);
            }
        }
    }
}
