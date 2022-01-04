package trace.util

class Production {
    inner class Rule(val left: String, val right: String) {
        fun apply(a: String): String {
            val r = Regex(Regex.escape(left))
            return r.replace(a, right)
        }
    }

    inner class Action(val m: String, val r: () -> Unit) {
        fun apply(a: String): String {
            val r = Regex("^" + Regex.escape(m))
            print(r)
            return r.replace(a, {mr ->
                r()
                ""})
        }
    }

    val rules = mutableListOf<Rule>()
    val actions = mutableListOf<Action>()

    fun addRule(left: String, right: String) {
        rules.add(Rule(left, right))
    }

    fun addAction(matches: String, action: () -> Unit) {
        actions.add(Action(matches, action))
    }

    fun clearRules() {
        rules.clear()
    }

    fun apply(a: String): String {
        var aa = a
        rules.forEach {
            println("aa => $aa")
            aa = it.apply(aa)
            if (aa.length > 1000) return aa
        }
        return aa
    }

    fun act(a: String) {
        var aa = a
        var prev = a
        while (aa.length > 0) {
            actions.forEach {
                aa = it.apply(aa)
            }

            if (aa.equals(prev)) {
                if (aa.length == 0)
                    return

                aa = aa.substring(1);
            }
            prev = aa;
        }
    }


}