package net.aquadc.decouplex.example;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import net.aquadc.decouplex.DecouplexBuilder;
import net.aquadc.decouplex.R;
import net.aquadc.decouplex.adapter.RetrofitResultAdapter;
import net.aquadc.decouplex.android.DecouplexActivity;
import net.aquadc.decouplex.annotation.OnResult;

import java.util.Locale;
import java.util.Random;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class SampleActivity extends DecouplexActivity {

    private GitHubService gitHubRetrofitService =
            new Retrofit.Builder()
                    .client(new OkHttpClient.Builder()
                            .addInterceptor(new HttpLoggingInterceptor()
                                    .setLevel(HttpLoggingInterceptor.Level.BODY)).build())
                    .baseUrl("https://api.github.com/")
                    .addConverterFactory(JacksonConverterFactory.create())
                    .build()
                    .create(GitHubService.class);

    private SampleService sampleService =
            new DecouplexBuilder<SampleService>()
                    .face(SampleService.class)
                    .impl(new SampleServiceImpl())
                    .create(DecouplexTestApp.getInstance());

    private GitHubService gitHubService =
            new DecouplexBuilder<GitHubService>()
                    .face(GitHubService.class)
                    .impl(gitHubRetrofitService)
                    .resultAdapter(new RetrofitResultAdapter())
                    .create(DecouplexTestApp.getInstance());

    private TextView result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_example);

        result = (TextView) findViewById(R.id.result);
    }

    public void sqr(View v) {
        sampleService.getSquare(new Random().nextInt());
    }

    public void gitHub(View v) {
        gitHubService.listRepos("Miha-x64");
    }

    @OnResult(face = SampleService.class, method = "getSquare")
    protected void onSquareGot(int i) {
        result.setText(String.format(Locale.getDefault(), "result: %s", i));
    }

    @OnResult(face = GitHubService.class, method = "listRepos")
    protected void onReposListed(Object o) {
        Toast.makeText(this, o.toString(), Toast.LENGTH_SHORT).show();
    }
}
