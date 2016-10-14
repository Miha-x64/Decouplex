### Decouplex ###
Decoupled executor — the easiest &amp; type-safe way to run code in Android service

#### Include via Gradle ####

[ ![Download](https://api.bintray.com/packages/miha-x64/maven/Decouplex/images/download.svg) ](https://bintray.com/miha-x64/maven/Decouplex/_latestVersion) — just Decouplex: `compile 'net.aquadc.decouplex:decouplex:0.0.2'`

[ ![Download](https://api.bintray.com/packages/miha-x64/maven/Decouplex-Retrofit/images/download.svg) ](https://bintray.com/miha-x64/maven/Decouplex-Retrofit/_latestVersion) — DecouplexRetrofit: `compile 'net.aquadc.decouplex:decouplexRetrofit:0.0.2'`

#### To do in next releases ####

- Support DecouplexBatchRequest.retry & document DecouplexBatch;
- Allow some methods to be called without result or error delivery;
- Develop `REMOTE` `DeliveryStrategy` to use implementation that runs in another process;
- Static factory with default parameter values to use in Kotlin without Builder.

#### Currently developing 0.0.3 ####
- Support nullable @OnResult and @OnError methods' arguments;

#### What's new in 0.0.2 ####

- DeliveryStrategy: a way to transfer arguments from Fragment to Service.
The only one is available for now — `DeliveryStrategy.LOCAL`.
Method arguments and return value will now be transferred out of Bundle and there's
no need for them to be Parcelable;
- Adapters' API changed: they aren't dependent on Bundle any more;
- A bug that caused a crash on SocketTimeoutException delivery was fixed
(actually, any exception delivery would be failed if this exception provides no message
but @OnError method requires it).

#### Since 0.0.1 ####

- Arguments and return values delivery;
- Wildcard result/error handlers (e. g. @OnResult("list*") works with all methods
which names start with "list");
- `@Debounce(delay)`.

#### Usage ####

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
Of course, GitHubService in this example is a Retrofit2-compatible interface:
```java
public interface GitHubService {
    @GET("users/{user}/repos")
    Call<List<Repo>> listRepos(@Path("user") String user);
}
```


You have to add service in manifest:
```xml
<service android:name="net.aquadc.decouplex.DecouplexService" />
```


When your class does not extend DecouplexActivity or DecouplexFragment,
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