# Decouplex
Decoupled executor — the easiest &amp; type-safe way to run code in Android service

You can do like this:
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
    
}
```
