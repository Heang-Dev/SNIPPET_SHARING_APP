package group.eleven.snippet_sharing_app.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import group.eleven.snippet_sharing_app.api.ApiClient;
import group.eleven.snippet_sharing_app.api.ApiService;
import group.eleven.snippet_sharing_app.data.model.ApiResponse;
import group.eleven.snippet_sharing_app.data.model.Collection;
import group.eleven.snippet_sharing_app.data.model.Snippet;
import group.eleven.snippet_sharing_app.data.model.SnippetCard;
import group.eleven.snippet_sharing_app.utils.Resource;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CollectionsRepository {

    private final ApiService apiService;

    public CollectionsRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
    }

    public LiveData<Resource<List<Collection>>> getMyCollections() {
        MutableLiveData<Resource<List<Collection>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        apiService.getCollections().enqueue(new Callback<ApiResponse<List<Collection>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Collection>>> call, Response<ApiResponse<List<Collection>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    result.setValue(Resource.success(response.body().getData()));
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "Failed to load collections";
                    result.setValue(Resource.error(msg, null));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Collection>>> call, Throwable t) {
                result.setValue(Resource.error("Network error: " + t.getMessage(), null));
            }
        });

        return result;
    }

    public LiveData<Resource<List<SnippetCard>>> getCollectionSnippets(String collectionId) {
        MutableLiveData<Resource<List<SnippetCard>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        Map<String, String> params = new HashMap<>();
        params.put("per_page", "50");

        apiService.getCollectionSnippets(collectionId, params).enqueue(new Callback<ApiResponse<List<Snippet>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Snippet>>> call, Response<ApiResponse<List<Snippet>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Snippet> snippets = response.body().getData();
                    List<SnippetCard> cards = new ArrayList<>();
                    if (snippets != null) {
                        for (Snippet s : snippets) cards.add(s.toSnippetCard());
                    }
                    result.setValue(Resource.success(cards));
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "Failed to load snippets";
                    result.setValue(Resource.error(msg, null));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Snippet>>> call, Throwable t) {
                result.setValue(Resource.error("Network error: " + t.getMessage(), null));
            }
        });

        return result;
    }
}
