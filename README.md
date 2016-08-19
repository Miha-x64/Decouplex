# Decouplex
Decoupled executor — the easiest &amp; type-safe way to run code in Android service

You can write your code like this:
```java
class SampleFragment extends DecouplexFragment {

    private GitHubService gitHubService;

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
            gitHubService = DecouplexBuilder
                    .retrofit2(getActivity(),
                            GitHubService.class, gitHubRetrofitService, getClass());
        }
    }
    
    // OnClick
    public void myGitHub() {
        enableUi(false);
        gitHubService.listRepos("Miha-x64");
    }
    
    @OnResult("listRepos")
    protected void onReposListed(List<Repo> repos) {
        resultView.setText(formatRepos(repos));
        enableUi(true);
    }
    
    // some code
    // setting OnClickListeners
    // enableUi & formatRepos methods
    // and other presenter logic
    
}
```
Of course, GitHubService in this example is a Retrofit-compatible interface:
```java
public interface GitHubService {
    @GET("users/{user}/repos")
    Call<List<Repo>> listRepos(@Path("user") String user);
}
```
When not extending DecouplexActivity or DecouplexFragment,
you can register & unregister BroadcastReceiver wherever you need:
in onCreate/onDestroy, onStart/onStop or onResume/onPause.

```java
class MyClass extends Fragment or Activity {

    private DecouplexReceiver decouplexReceiver;

    @Override
    protected void onStart() {
        super.onStart();
        if (decouplexReceiver == null)
            decouplexReceiver = new DecouplexReceiver(this);
        decouplexReceiver.register();
    }

    @Override
    protected void onStop() {
        decouplexReceiver.unregister();
        super.onStop();
    }
}
```