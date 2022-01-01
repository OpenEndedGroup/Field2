package fieldnashorn

import field.app.RunLoop
import field.app.ThreadSync2
import field.utility.Dict
import field.utility.Options

class Watchdog
{
    var frame = RunLoop.tick
    var frameAt = System.currentTimeMillis()
    var success = 0

    val warning = 2000
    val error = 5000
    var warned = false

    fun tick()
    {
        if (!limits) return;
        if (RunLoop.tick!=frame)
        {
            frame = RunLoop.tick
            success++
            frameAt = System.currentTimeMillis()
            warned = false
            return
        }
        val danger = System.currentTimeMillis()-frameAt
        if (danger>warning && !warned)
        {
            System.err.println(" warning: Watchdog notices frame is no longer advancing for "+danger+"ms "+success)
            warned = true
        }
        if (danger>error && success>0)
        {
            System.err.println(" error: Watchdog notices frame is no longer advancing for "+danger+"ms")
            throw ThreadSync2.TooLongKilledException("Timer expired, your code is taking too long to complete")
        }
    }

    companion object {
        @JvmStatic
        var limits = Options.dict().isTrue(Dict.Prop<Number>("noLimits"), false)==false

        @JvmStatic
        fun limit(n : Int, max : Int, message : String?)
        {
            if (!limits) return;
            if (n>max && max>-1)
                throw ThreadSync2.TooLongKilledException("Resource limit exceeded '"+message+"'")
        }
    }
}