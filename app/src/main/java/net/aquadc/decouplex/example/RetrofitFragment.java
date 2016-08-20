package net.aquadc.decouplex.example;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.aquadc.decouplex.DecouplexBatch;
import net.aquadc.decouplex.DecouplexBuilder;
import net.aquadc.decouplex.DecouplexFragmentCompat;
import net.aquadc.decouplex.DecouplexRequest;
import net.aquadc.decouplex.DecouplexRetrofit;
import net.aquadc.decouplex.annotation.OnError;
import net.aquadc.decouplex.annotation.OnResult;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * Created by miha on 24.05.16.
 *
 */
public class RetrofitFragment extends DecouplexFragmentCompat implements View.OnClickListener {

    /**
     * Presenter
     */

    private TextView resultView;
    private View button0, button1, button2;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_retrofit, container, false);

        resultView = (TextView) view.findViewById(R.id.result);

        button0 = view.findViewById(R.id.button0);
        button1 = view.findViewById(R.id.button1);
        button2 = view.findViewById(R.id.button2);

        button0.setOnClickListener(this);
        button1.setOnClickListener(this);
        button2.setOnClickListener(this);

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("UIEnabled", button0.isEnabled());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            enableUi(savedInstanceState.getBoolean("UIEnabled", true));
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button0:
                myGitHub();
                break;

            case R.id.button1:
                squareGitHub();
                break;

            case R.id.button2:
                longRunningTask();
                break;
        }
    }

    private void enableUi(boolean enable) {
        button0.setEnabled(enable);
        button1.setEnabled(enable);
        button2.setEnabled(enable);
    }

    /**
     * Controller
     */

    private GitHubService gitHubService;
    private LongRunningTask longRunningTask;
    private DecouplexBatch<?> batch;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (gitHubService == null) {
            // configure Retrofit
            GitHubService gitHubRetrofitService =
                    new Retrofit.Builder()
                            .baseUrl("https://api.github.com/")
                            .addConverterFactory(JacksonConverterFactory.create())
                            .build()
                            .create(GitHubService.class);

            // configure Decouplex
            gitHubService = DecouplexRetrofit
                    .retrofit2(getActivity(),
                            GitHubService.class, gitHubRetrofitService, getClass());

            // configure another Decouplex
            longRunningTask =
                    new DecouplexBuilder<>(LongRunningTask.class, new LongRunningTaskImpl(), getClass())
                            .create(getActivity());

            batch = new DecouplexBatch<>(getClass());
        }
    }

    @Override
    public void onDetach() {
        gitHubService = null;
        longRunningTask = null;
        batch = null;
        super.onDetach();
    }

    // Retrofit

    public void myGitHub() {
        enableUi(false);
        gitHubService.listRepos("Miha-x64");
    }

    public void squareGitHub() {
        enableUi(false);
        gitHubService.listRepos("square");
    }

    @OnResult("listRepos")
    protected void onReposListed(List<Repo> repos) {
        resultView.setText(formatRepos(repos));
        enableUi(true);
    }

    @OnError
    protected void onError(final DecouplexRequest failedRequest, Exception e, int code, String message) {
        if (code == 0) {
            // network problem
            Snackbar.make(button0, e.toString(), Snackbar.LENGTH_LONG)
                    .setAction(R.string.snackbar_action_retry, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            enableUi(false);
                            failedRequest.retry(getActivity());
                        }
                    })
                    .show();
        } else {
            // http error
            Snackbar.make(button0, code + ": " + message, Snackbar.LENGTH_LONG).show();
        }
        enableUi(true);
    }

    // Long running task

    public void longRunningTask() {
        enableUi(false);
        batch.add(gitHubService).listRepos("Miha-x64");
        batch.add(longRunningTask).calculateFactorial(BigInteger.valueOf(new Random().nextInt(50)));
        batch.add(longRunningTask).slowDown();
        batch.start(getActivity());
    }

    @OnResult("listRepos, calculateFactorial, slowDown")
    protected void onAllFinished(List<Repo> repos, BigInteger fact) {
        SpannableStringBuilder ssb = (SpannableStringBuilder) formatRepos(repos);
        ssb.append("\n\n").append("random! = ").append(NumberFormat.getNumberInstance().format(fact));
        resultView.setText(ssb);
        enableUi(true);
    }

    /**
     * Misc
     */

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

            sb.append("\n");
            if (repo.description != null) {
                sb.append(repo.description);
            }

            if (iterator.hasNext())
                sb.append("\n\n");
            else
                break;
        }
        return sb;
    }

}
