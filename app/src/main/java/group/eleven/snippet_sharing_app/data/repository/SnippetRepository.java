package group.eleven.snippet_sharing_app.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import group.eleven.snippet_sharing_app.api.ApiClient;
import group.eleven.snippet_sharing_app.api.ApiService;
import group.eleven.snippet_sharing_app.data.model.ApiResponse;
import group.eleven.snippet_sharing_app.data.model.Snippet;
import group.eleven.snippet_sharing_app.utils.Resource;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SnippetRepository {

    private final ApiService apiService;

    public SnippetRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
    }

    public LiveData<Resource<Snippet>> getSnippetBySlug(String slug) {
        MutableLiveData<Resource<Snippet>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        apiService.getSnippetBySlug(slug).enqueue(new Callback<ApiResponse<Snippet>>() {
            @Override
            public void onResponse(Call<ApiResponse<Snippet>> call, Response<ApiResponse<Snippet>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    result.setValue(Resource.success(response.body().getData()));
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "Failed to load snippet";
                    result.setValue(Resource.error(msg, null));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Snippet>> call, Throwable t) {
                result.setValue(Resource.error("Network error: " + t.getMessage(), null));
            }
        });

        return result;
    }
}
