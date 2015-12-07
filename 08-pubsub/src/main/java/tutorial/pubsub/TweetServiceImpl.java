package tutorial.pubsub;

import javax.inject.Inject;

import io.baratine.core.Journal;
import io.baratine.core.Lookup;
import io.baratine.core.OnActive;
import io.baratine.core.Result;
import io.baratine.core.ResultStream;
import io.baratine.core.Service;
import io.baratine.pubsub.PubSubService;

@Journal
@Service("public:///tweet")
public class TweetServiceImpl
{
  @Inject @Lookup("pubsub:///my-topic")
  private PubSubService<String> _pubsub;

  public void subscribe(ResultStream<String> result)
  {
    _pubsub.subscribe(result);
  }

  public void publish(String msg)
  {
    _pubsub.publish(msg);
  }

  @OnActive
  public void onActive(Result<Void> result)
  {
    System.out.println(getClass().getSimpleName() + " service is now active");

    result.complete(null);
  }
}
