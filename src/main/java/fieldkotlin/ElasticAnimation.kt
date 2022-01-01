package fieldkotlin

class ElasticAnimation : Runnable {
    companion object {

        suspend fun SequenceScope<Any>.yield() {
            yield(true)
        }

        val _time = ThreadLocal<Double>()

        var time: Double
            get() = _time.get()
            set(n: Double) = _time.set(n)

        fun <T> atTime(t: Double, b: () -> T): T {
            val was = time
            try {
                time = t
                if (time.isNaN()) time = 1.0
                if (time.isInfinite()) time = 1.0
                if (time < -0.0) {
                    println(" warning: unexpected negative time $time")
                    time = 0.0
                }
                return b()
            } finally {
                time = was
            }
        }

        val _block = ThreadLocal<Block>()

        var block: Block
            get() = _block.get()
            set(n) = _block.set(n)

        fun <T> inBlock(t: Block, b: () -> T): T {
            val was = _block.get()
            try {

                val realTime = if (_block.get() == null) time else time * was.duration + was.effectiveStart
                val newTime = (realTime - t.effectiveStart) / t.duration

                return atTime(newTime) {
                    block = t
                    b()
                }
            } finally {
                _block.set(was)
            }
        }
    }


    class Block(
        var start: Int = 0,
        var startConstraint: ((Block?, Block, List<Block>) -> Int)? = null,
        var duration: Int = -1,
        var durationConstraint: ((Block?, Block, List<Block>) -> Int)? = null,
        val name: String = "",
        val apply: suspend SequenceScope<Any>.() -> Unit
    ) {
        // children for the purposes of figuring out the duration
        // start of these is relative to the start of this
        val childrenDuration = mutableListOf<Block>()
        val childrenStart = mutableListOf<Block>()
        val children = mutableListOf<Block>()

        var parent: Block? = null


        var effectiveStart = 0

        var seq = sequence(apply).iterator()

        fun reset() {
            seq = sequence(apply).iterator()
        }

        fun and(b: BlockBuilder.() -> Unit): Block {
            BlockBuilder(this).apply(b)
            return this
        }

        fun preFixDurations() {
            startConstraint?.let {
//                if (childrenStart.size > 0)
                    start = it(parent, this, childrenStart)
            }
        }

        fun postFixDurations() {
            durationConstraint?.let {
                duration = it(parent, this, childrenDuration)
                if (duration < 0) duration = 0
            }

        }

        fun runAtTime(realTime: Double): Boolean {
            _block.set(null)
            time = realTime
            return inBlock(this) {
                if (!seq.hasNext())
                    false
                else {
                    seq.next()
                    true
                }
            }
        }

        fun runAtAlpha(alpha: Double): Boolean {
            _block.set(null)
            time = start + duration * alpha
            return inBlock(this) {
                if (!seq.hasNext())
                    false
                else {
                    seq.next()
                    true
                }
            }
        }


        override fun toString(): String {
            if (name.length > 0) {
                return "'${name}'[${start}->+${duration}](${childrenDuration.size})"
            }
            return "[${start}->+${duration}](${childrenDuration.size})"
        }

        operator fun plus(b: BlockBuilder.() -> Unit): Block {
            BlockBuilder(this).apply(b)
            return this
        }
    }

    class BlockBuilder(val scope: Block) {

        fun block(
            start: Int,
            startConstraint: ((Block?, Block, List<Block>) -> Int)? = null,
            duration: Int = -1,
            durationConstraint: ((Block?, Block, List<Block>) -> Int)? = null,
            name: String = "",
            apply: suspend SequenceScope<Any>.() -> Unit
        ) =
            Block(
                start = start,
                startConstraint = startConstraint,
                duration = duration,
                durationConstraint = durationConstraint,
                name = name,
                apply = apply
            ).also {
                scope.childrenDuration.add(it)
                scope.children.add(it)
                it.parent = scope
            }

        fun event(start: Int, name: String = "", apply: suspend SequenceScope<Any>.() -> Unit) =
            block(start = start, duration = 0, durationConstraint = { _, _, _ -> 0 }, name = name, apply = apply)

        fun gap(start: Int, name: String = "", apply: suspend SequenceScope<Any>.() -> Unit) =
            block(
                start = start,
                duration = 0,
                durationConstraint = { _, _, c -> c.minOf { it.start } },
                name = name,
                apply = apply
            )

        fun over(start: Int, name: String = "", apply: suspend SequenceScope<Any>.() -> Unit) =
            block(
                start = start,
                duration = 0,
                durationConstraint = { _, _, c -> c.maxOf { it.start + it.duration } },
                name = name,
                apply = apply
            )

        fun overAll(start: Int, name: String = "", apply: suspend SequenceScope<Any>.() -> Unit) =
            block(
                start = start,
                duration = 0,
                durationConstraint = { _, _, c -> c.allChildren().maxOf { it.start + it.duration } },
                name = name,
                apply = apply
            )


        fun joinParent(start: Int = 0, name: String = "", apply: suspend SequenceScope<Any>.() -> Unit) =
            block(
                start = start,
                duration = 0,
                durationConstraint = { p, _, _ -> p!!.duration - start },
                name = name,
                apply = apply
            )

        fun then(duration: Int, name: String = "", apply: suspend SequenceScope<Any>.() -> Unit) = Block(
            start = 0,
            startConstraint = { p, t, _ -> p!!.duration  },
            duration = duration,
            name = name,
            apply = apply
        ).also {
            scope.childrenStart.add(it)
            scope.children.add(it)
            it.parent = scope
        }


    }

    fun blocks(b: BlockBuilder.() -> Unit): Block {
        val r = Block(
            start = 0,
            duration = 0,
            durationConstraint = { _, _, c -> c.allChildren().maxOfOrNull { it.start + it.duration }?:0 },
            name = "anonymous block"
        ) {}
        BlockBuilder(r).apply(b)
        return r
    }

    fun linearize(b: Block, o: Int = 0): Sequence<Block> = sequence<Block> {
        b.preFixDurations()
        b.effectiveStart = b.start + o
        yield(b)

        b.children.forEach {
            yieldAll(linearize(it, o = b.effectiveStart))
        }
        b.postFixDurations()
    }

    fun events(l: List<Block>): List<Pair<String, List<Block>>> {

        val seen = mutableMapOf<Pair<Int, String>, MutableList<Block>>()

        val r = mutableListOf<Pair<String, List<Block>>>()

        l.forEach {

            val n = it.name
            val t = it.effectiveStart

            if (seen.containsKey(t to n))
                seen[t to n]!!.add(it)
            else {
                val ll = mutableListOf<Block>()
                seen[t to n] = ll
                ll.add(it)
                r.add(n to ll)
            }

        }


        return r

    }

    fun run(l: Block) = run(listOf(l))
    fun run(l: BlockBuilder) = run(listOf(l.scope))

    fun run(l: List<Block>): Block {
        val all = l.flatMap { linearize(it) }
        all.forEach {
            it.reset()
        }
        val e = events(all)

        val first = e.minOfOrNull { it.second[0].effectiveStart } ?: 0
        val last = e.maxOfOrNull { it.second.maxOf { it.effectiveStart + it.duration } } ?: 0



        return Block(start = first, duration = last - first, name = "runner") {

            // we're going to go through all events and check to see if they need to be run, but we can do this incrementally
            val ongoing = mutableListOf<Block>()

            var previousTime = -1.0
//            var previousCursor = 0

            val dontCall = mutableMapOf<Block, Int>()
            while (time <= 1) {
                // scan for events that fall into the gap
                val realTime = time * block.duration + block.effectiveStart
                val previousRealTime = previousTime * block.duration + block.effectiveStart
                e.flatMap { it.second }.filter { it.effectiveStart > previousRealTime && it.effectiveStart <= realTime }
                    .toCollection(ongoing) // this could be faster
                previousTime = time

                ongoing.removeIf { block ->

                    if (dontCall.containsKey(block)) {
                        var a = dontCall[block]!!
                        if (a > 0)
                            dontCall[block] = a - 1
                        else
                            dontCall.remove(block)
                        false
                    } else
                        inBlock(block) {
                            if (block.seq.hasNext()) {
                                val un = block.seq.next()

                                when (un) {
                                    is Int -> {
                                        if (un > 0)
                                            dontCall[block] = un - 1
                                    }
                                    is Boolean -> {
                                        return@inBlock !un
                                    }
                                }
                                time >= 1
                            } else true
                        }
                }
                yield(1)
            }
            ongoing.forEach {
                time = Double.POSITIVE_INFINITY
                inBlock(it) {
                    if (block.seq.hasNext()) {
                        val un = block.seq.next()
                    }
                }
            }
        }
    }

    override fun run() {
        System.`in`.read()

        println(" -- starting up --")

        val r: Block = blocks {

            block(0, duration = 100) {
                print("hello at time ${time}")
                while (time < 1) {
                    println(" -- tick ${time}")
                    yield()
                }
                println(" -- its over!")
            } + {

                overAll(30) {
                    print("hello2 at time ${time}")
                    while (time < 1) {
                        println(" -- tick2 ${time}")
                        yield(1)
                    }
                    println(" -- its2 over!")
                } + {
                    block(30, duration = 0) {
                        print("event called with ${time}")
                        while (time < 1) {
                            yield(1)
                        }
                        println(" -- event should be over!")
                        for (n in 0 until 10) {
                            yield(true)
                            println(" -- event should be over $n / 10!")
                        }
                    }
                }

                joinParent(110, name = "flurrish") {
                    do {
                        println(" a flurrish at the end $time ...?")
                        yield()
                    } while (time < 1)
                }


            }
        }

        val blocks = mutableListOf(r)

        println(" -- linearizing --")
        val root = run(blocks)
        println(" -- running --")
        for (n in 0 until 150) {
            println(" --                    frame ${n}>")
            root.runAtTime(n.toDouble())
        }
        println(" -- finished --")

    }

}

fun List<ElasticAnimation.Block>.allChildren(): List<ElasticAnimation.Block> {
    var a = mutableListOf<ElasticAnimation.Block>()
    a.addAll(this)
    forEach {
        a.addAll(it.childrenDuration.allChildren())
    }
    return a
}


