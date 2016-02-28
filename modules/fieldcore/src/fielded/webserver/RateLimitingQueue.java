package fielded.webserver;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by marc on 3/29/14.
 */
public abstract class RateLimitingQueue<t_group, T> {

	BlockingQueue<T> q = new LinkedBlockingQueue<>();

	float target;
	float sleep = 50;

	public RateLimitingQueue(float targetPerSecond, float maxLatency)
	{
		this.target = targetPerSecond;

		new Thread(() -> {
			long enter = System.currentTimeMillis();
			int num = 0;

			while(true)
			{
				if (q.isEmpty())
				{

				}
				else
				{
					List<T> m = new ArrayList<>();
					q.drainTo(m);
					num+=coalleseAndSend(m);
				}

				long now = System.currentTimeMillis();
				if (now-enter>1000)
				{
					float currentRate = 1000f*num/(now-enter);
					if (currentRate>targetPerSecond) sleep*=1.5f;
					else sleep/=1.5f;
					enter = System.currentTimeMillis();
					num = 0;

					if (sleep>maxLatency) sleep=maxLatency;
					if (sleep<1) sleep=1;
				}
				try {
					Thread.sleep((long) sleep);
				} catch (InterruptedException e) {
				}
			}
		}).start();
	}

	public void add(T t)
	{
		q.add(t);
	}

	protected int coalleseAndSend(List<T> q)
	{
		Multimap<t_group, T> groups = MultimapBuilder.linkedHashKeys().linkedListValues().build();
		for(T t : q)
		{
			t_group g = groupFor(t);
			groups.put(g, t);
		}

		groups.asMap().entrySet().forEach(x -> {
			send(x.getKey(), x.getValue());
		});

		return groups.size();
	}

	protected abstract t_group groupFor(T t);

	protected abstract void send(t_group key, Collection<T> value);
}
