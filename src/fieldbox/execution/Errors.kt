package fieldbox.execution

import com.google.common.collect.MapMaker
import field.utility.Log
import field.utility.Pair
import fieldbox.boxes.Box
import fieldbox.io.IO
import fielded.RemoteEditor

import java.util.ArrayList
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier
import java.util.regex.Pattern

/**
 * A Fundamental problem in sending code off for execution (remotely, in draw loops or other callbacks) is what you do when it throws an exception. This is particularly bad in the case of a draw loop --- you might
 * only catch an OpenGL error long after some code has executed. Therefore we often push things onto this stack and leave them there.
 */
object Errors {


	@JvmStatic
	fun tryToReportTo(e: Throwable, additionalMessage: String, offendingObject: Any?) {

		val description = InverseDebugMapping.describe(offendingObject)
		findResponsibleBox(InverseDebugMapping.defaultRoot, e, additionalMessage + (if (description != null) "\n" + description else ""))
	}

	fun <T> handle(o: Supplier<T>, message: Function<Throwable, T>): T {
		try {
			return o.get()
		} catch (t: Throwable) {
			return message.apply(t)
		}
	}

	internal var boxFinder = Pattern.compile("bx\\[(.+)\\]/([_0123456789abcdef]+)")

	private fun findResponsibleBox(root: Box, th: Throwable, additionalMessage: String) {
//		println("scouring stacktrace")
		for (t in th.stackTrace) {
//			println(t.fileName + " || " + t.lineNumber + " || " + t.className + " || " + t.methodName + " ||    " + t)
			val m = boxFinder.matcher(t.fileName)
			if (m.matches()) {
				val uid = m.group(2)
				val name = m.group(1)
				reportError(root, uid, name, t, t.lineNumber, th, additionalMessage)
				return
			}
		}


	}

	private fun reportError(defaultRoot: Box, uid: String, name: String, t: StackTraceElement, lineNumber: Int, tr: Throwable, additionalMessage: String) {

		val target = defaultRoot.breadthFirst(defaultRoot.allDownwardsFrom()).filter { it.properties.get(IO.id)?.equals(uid) ?: false }.findFirst()
		if (!target.isPresent) {
			System.err.println(" Exception thrown in a box called ${name} which can't be found any more; full stacktrace below:")
			if (additionalMessage != null) System.err.println(additionalMessage)
			tr.printStackTrace()
		} else {
			val oef = target.get().first(RemoteEditor.outputErrorFactory)
			if (!oef.isPresent) {
				System.err.println(" Exception thrown in a box called ${name} which isn't editable; full stacktrace below:")
				if (additionalMessage != null) System.err.println(additionalMessage)
				tr.printStackTrace()
			} else {
				System.err.println(" Exception thrown in a box called ${name}, also reported to the editor")
				if (additionalMessage != null) System.err.println(additionalMessage)
				tr.printStackTrace()
				oef.ifPresent { it.apply(target.get()).accept(Pair(lineNumber, (if (additionalMessage != null) additionalMessage.trim() + "\n" else "") + tr.message)) }
			}
		}

	}
}
