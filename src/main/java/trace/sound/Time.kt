package trace.sound

class Time
{
    var born = System.currentTimeMillis()

    fun now(): Double {
        return (System.currentTimeMillis()-born)/1000.0
    }

    fun reset()
    {
        born = System.currentTimeMillis()
    }

    fun set(now : Double) {
        born = (System.currentTimeMillis()-now*1000).toLong()
    }
}
