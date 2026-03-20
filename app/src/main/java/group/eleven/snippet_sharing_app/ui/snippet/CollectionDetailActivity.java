package group.eleven.snippet_sharing_app.ui.snippet;

import android.content.Intent;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import group.eleven.snippet_sharing_app.R;
import group.eleven.snippet_sharing_app.data.repository.CollectionsRepository;
import group.eleven.snippet_sharing_app.ui.home.SnippetCardAdapter;
import group.eleven.snippet_sharing_app.utils.Resource;

public class CollectionDetailActivity extends AppCompatActivity {

    public static final String EXTRA_COLLECTION_ID = "extra_collection_id";
    public static final String EXTRA_COLLECTION_NAME = "extra_collection_name";

    private CollectionsRepository collectionsRepository;
    private SnippetCardAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection_detail);

        collectionsRepository = new CollectionsRepository(this);

        setupStatusBar();
        setupToolbar();

        String collectionId = getIntent().getStringExtra(EXTRA_COLLECTION_ID);
        String collectionName = getIntent().getStringExtra(EXTRA_COLLECTION_NAME);

        if (collectionId == null) {
            finish();
            return;
        }

        TextView tvCollectionTitle = findViewById(R.id.tvCollectionTitle);
        tvCollectionTitle.setText(collectionName != null ? collectionName : "Collection");

        setupRecyclerView(collectionId);
        loadSnippets(collectionId);
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
    }

    private void setupRecyclerView(String collectionId) {
        RecyclerView rvSnippets = findViewById(R.id.rvSnippets);
        rvSnippets.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SnippetCardAdapter(new ArrayList<>());
        adapter.setOnSnippetClickListener(snippet -> {
            String id = snippet.getSlug() != null ? snippet.getSlug() : snippet.getId();
            if (id != null) {
                Intent intent = new Intent(this, SnippetDetailActivity.class);
                intent.putExtra(SnippetDetailActivity.EXTRA_SNIPPET_ID, id);
                startActivity(intent);
            }
        });
        rvSnippets.setAdapter(adapter);
    }

    private void loadSnippets(String collectionId) {
        collectionsRepository.getCollectionSnippets(collectionId).observe(this, resource -> {
            FrameLayout layoutLoading = findViewById(R.id.layoutLoading);
            RecyclerView rvSnippets = findViewById(R.id.rvSnippets);
            LinearLayout layoutEmpty = findViewById(R.id.layoutEmpty);

            layoutLoading.setVisibility(View.GONE);

            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                if (resource.data.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    rvSnippets.setVisibility(View.GONE);
                } else {
                    adapter.filterList(resource.data);
                    rvSnippets.setVisibility(View.VISIBLE);
                    layoutEmpty.setVisibility(View.GONE);
                }
            } else if (resource.status == Resource.Status.ERROR) {
                layoutEmpty.setVisibility(View.VISIBLE);
                rvSnippets.setVisibility(View.GONE);
                Toast.makeText(this, "Failed to load snippets", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
