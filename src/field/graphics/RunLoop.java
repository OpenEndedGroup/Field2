package field.graphics;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by marc on 3/25/14.
 */
public class RunLoop {

	static public final RunLoop main = new RunLoop();
	static public long tick = 0;

	public Scene mainLoop = new Scene();
	Thread mainThread = null;
	static public final ExecutorService workerPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 2);


	public Scene getLoop()
	{
		return mainLoop;
	}

	public boolean isMainThread()
	{
		return Thread.currentThread()==mainThread;
	}

	public void enterMainLoop()
	{
		mainThread = Thread.currentThread();
		while(true)
		{
			try{
				tick++;
				mainLoop.updateAll();
				Thread.sleep(1);
			}
			catch(Throwable t)
			{
				System.err.println(" exception thrown in main loop");
				t.printStackTrace();
			}
		}
	}

	public void once(Runnable r)
	{
		mainLoop.connect( i -> {r.run(); return false;});
	}

}
