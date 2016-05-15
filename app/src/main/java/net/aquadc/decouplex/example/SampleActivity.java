package net.aquadc.decouplex.example;

import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import net.aquadc.decouplex.DecouplexBuilder;
import net.aquadc.decouplex.R;
import net.aquadc.decouplex.android.DecouplexActivity;
import net.aquadc.decouplex.annotation.OnError;
import net.aquadc.decouplex.annotation.OnResult;

import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class SampleActivity extends DecouplexActivity {

    private final GitHubService gitHubRetrofitService =
            new Retrofit.Builder()
                    .client(new OkHttpClient.Builder()
                            .addInterceptor(new HttpLoggingInterceptor()
                                    .setLevel(HttpLoggingInterceptor.Level.BODY)).build())
                    .baseUrl("https://api.github.com/")
                    .addConverterFactory(JacksonConverterFactory.create())
                    .build()
                    .create(GitHubService.class);

    private final GitHubService gitHubService =
            DecouplexBuilder
                    .retrofit2(DecouplexTestApp.getInstance(),
                            GitHubService.class, gitHubRetrofitService);

    private TextView result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_example);

        result = (TextView) findViewById(R.id.result);
    }

    public void gitHub(View v) {
        gitHubService.listRepos("Miha-x64");
    }

    @OnResult(face = GitHubService.class, method = "listRepos")
    protected void onReposListed(List<Repo> repos, int code) {
        StringBuilder sb = new StringBuilder();
        for (Repo repo : repos) {
            sb.append("<b>").append(repo.name).append("</b><br/>").append(repo.description).append("<br/><br/>");
        }
        result.setText(Html.fromHtml(sb.toString()));
        Toast.makeText(this, "resp code: " + code, Toast.LENGTH_SHORT).show();
    }

    @OnError(face = GitHubService.class, method = "listRepos")
    protected void onError(Exception e, int code, String message) {
        Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        Toast.makeText(this, code + ": " + message, Toast.LENGTH_SHORT).show();
    }
}
