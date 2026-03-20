package group.eleven.snippet_sharing_app.ui.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import group.eleven.snippet_sharing_app.R;
import group.eleven.snippet_sharing_app.data.model.Collection;

public class CollectionAdapter extends RecyclerView.Adapter<CollectionAdapter.ViewHolder> {

    private List<Collection> collections = new ArrayList<>();
    private OnCollectionClickListener listener;

    public interface OnCollectionClickListener {
        void onCollectionClick(Collection collection);
    }

    public void setOnCollectionClickListener(OnCollectionClickListener listener) {
        this.listener = listener;
    }

    public void setCollections(List<Collection> collections) {
        this.collections = collections != null ? collections : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_collection_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(collections.get(position));
    }

    @Override
    public int getItemCount() {
        return collections.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDescription, tvSnippetCount, tvPrivacy;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCollectionName);
            tvDescription = itemView.findViewById(R.id.tvCollectionDescription);
            tvSnippetCount = itemView.findViewById(R.id.tvSnippetCount);
            tvPrivacy = itemView.findViewById(R.id.tvPrivacy);
        }

        void bind(Collection collection) {
            tvName.setText(collection.getName());

            String desc = collection.getDescription();
            if (desc != null && !desc.isEmpty()) {
                tvDescription.setText(desc);
                tvDescription.setVisibility(View.VISIBLE);
            } else {
                tvDescription.setVisibility(View.GONE);
            }

            int count = collection.getSnippetsCount();
            tvSnippetCount.setText(count == 1 ? "1 snippet" : count + " snippets");

            tvPrivacy.setText(collection.isPublic() ? "Public" : "Private");

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onCollectionClick(collection);
            });
        }
    }
}
