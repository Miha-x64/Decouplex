package net.aquadc.decouplex.example;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import net.aquadc.decouplex.DecouplexBatch;
import net.aquadc.decouplex.DecouplexBuilder;
import net.aquadc.decouplex.R;
import net.aquadc.decouplex.DecouplexActivity;
import net.aquadc.decouplex.annotation.OnError;
import net.aquadc.decouplex.annotation.OnResult;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class SampleActivity extends DecouplexActivity {

    // Tasks
    private GitHubService gitHubService;
    private LongRunningTask longRunningTask;
    private DecouplexBatch<?> batch;

    // UI
    private TextView resultView;
    private View button0, button1, button2;

    // Lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (gitHubService == null) {
            // configure Retrofit
            GitHubService gitHubRetrofitService =
                    new Retrofit.Builder()
                            .baseUrl("https://api.github.com/")
                            .addConverterFactory(JacksonConverterFactory.create())
                            .build()
                            .create(GitHubService.class);

            // configure Decouplex
            gitHubService = DecouplexBuilder
                    .retrofit2(this,
                            GitHubService.class, gitHubRetrofitService, getClass());

            // configure another Decouplex
            longRunningTask =
                    new DecouplexBuilder<>(LongRunningTask.class, new LongRunningTaskImpl(), getClass())
                            .create(this);

            batch = new DecouplexBatch<>(getClass());
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        resultView = (TextView) findViewById(R.id.result);
        button0 = findViewById(R.id.button0);
        button1 = findViewById(R.id.button1);
        button2 = findViewById(R.id.button2);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("UIEnabled", button0.isEnabled());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            enableUi(savedInstanceState.getBoolean("UIEnabled", true));
        }
    }

    // Retrofit

    public void myGitHub(View v) {
        enableUi(false);
        gitHubService.listRepos("Miha-x64");
    }

    public void squareGitHub(View v) {
        enableUi(false);
        gitHubService.listRepos("square");
    }

    @OnResult("listRepos")
    protected void onReposListed(List<Repo> repos) {
        resultView.setText(formatRepos(repos));
        enableUi(true);
    }

    @OnError
    protected void onError(Exception e, int code, String message) {
        Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        Toast.makeText(this, code + ": " + message, Toast.LENGTH_SHORT).show();
        enableUi(true);
    }

    // Long running task

    public void longRunningTask(View v) {
        enableUi(false);
        batch.add(gitHubService).listRepos("Miha-x64");
        batch.add(longRunningTask).calculateFactorial(BigInteger.valueOf(new Random().nextInt(50)));
        batch.add(longRunningTask).slowDown();
        batch.start(this);
    }

    @OnResult("listRepos, calculateFactorial, slowDown")
    protected void onAllFinished(List<Repo> repos, BigInteger fact) {
        SpannableStringBuilder ssb = (SpannableStringBuilder) formatRepos(repos);
        ssb.append("\n\n").append("random! = ").append(NumberFormat.getNumberInstance().format(fact));
        resultView.setText(ssb);
        enableUi(true);
    }

    // Misc

    private void enableUi(boolean enable) {
        button0.setEnabled(enable);
        button1.setEnabled(enable);
        button2.setEnabled(enable);
    }

    private static CharSequence formatRepos(List<Repo> repos) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        Iterator<Repo> iterator = repos.iterator();
        while (true) {
            Repo repo = iterator.next();
            int start = sb.length();
            sb.append(repo.name);
            sb.setSpan(new StyleSpan(Typeface.BOLD),
                    start, start + repo.name.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (repo.fork) {
                sb.append(" (fork)");
            }
            sb.append("\n").append(repo.description);
            if (iterator.hasNext())
                sb.append("\n\n");
            else
                break;
        }
        return sb;
    }
}
